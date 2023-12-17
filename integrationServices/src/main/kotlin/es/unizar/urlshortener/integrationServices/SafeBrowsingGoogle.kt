package es.unizar.urlshortener.integrationServices

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.safebrowsing.Safebrowsing
import com.google.api.services.safebrowsing.model.ClientInfo
import com.google.api.services.safebrowsing.model.FindThreatMatchesRequest
import com.google.api.services.safebrowsing.model.ThreatEntry
import com.google.api.services.safebrowsing.model.ThreatInfo
import java.util.*
import java.util.Properties
import java.io.IOException
import es.unizar.urlshortener.core.SafeBrowsingService
import org.slf4j.LoggerFactory
/**
 * Verify if the url is safe with Google Safe Browsing.
 */

//private const val CONFIG = "config.properties"
val GOOGLE_JSON_FACTORY: JacksonFactory = JacksonFactory.getDefaultInstance()
val GOOGLE_THREAT_TYPES = listOf("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE",
    "POTENTIALLY_HARMFUL_APPLICATION")
val GOOGLE_PLATFORM_TYPES = listOf("ANY_PLATFORM")
val GOOGLE_THREAT_ENTRYTYPES = listOf("URL")
var httpTransport: NetHttpTransport? = null

class SafeBrowsingServiceImpl : SafeBrowsingService {
    val logger = LoggerFactory.getLogger(SafeBrowsingServiceImpl::class.java)

    companion object {
        private val properties: Properties = loadProperties()

        private fun loadProperties(): Properties {
            val properties = Properties()
            val logger = LoggerFactory.getLogger(SafeBrowsingServiceImpl::class.java)

            try {
                SafeBrowsingServiceImpl::class.java.getResourceAsStream("/config.properties").use { inputStream ->
                    if (inputStream != null) {
                        properties.load(inputStream)
                    } else {
                        logger.error("Unable to find 'config.properties'")
                    }
                }
            } catch (e: IOException) {
                logger.error("Error loading properties from 'config.properties'", e)
            }

            return properties
        }

        private val GOOGLE_API_KEY: String = properties.getProperty("google.api.key")
        private val GOOGLE_CLIENT_ID: String = properties.getProperty("google.client.id")
        private val GOOGLE_CLIENT_VERSION: String = properties.getProperty("google.client.version")
        private val GOOGLE_APPLICATION_NAME: String = properties.getProperty("google.application.name")
    }
    /**
     * Verifica si una URL es segura utilizando Google Safe Browsing.
     *
     * @param url La URL a verificar.
     * @return `true` si la URL es segura, `false` en caso contrario.
     */
    override fun isSafe(url: String): Boolean {
        return urlsAreSafe(listOf(url)).size == 0
    }
    /**
     * Verifica si una lista de URLs son seguras utilizando Google Safe Browsing.
     *
     * @param urls Lista de URLs a verificar.
     * @return Lista de URLs que se consideran no seguras.
     */
    override fun urlsAreSafe(urls: List<String>) : List<String> {
        logger.info("Comprobando URLs con Google Safe Browsing")
        httpTransport = GoogleNetHttpTransport.newTrustedTransport()

        val findThreatMatchesRequest: FindThreatMatchesRequest = createFindThreatMatchesRequest(urls)
        val safebrowsingBuilder =
            Safebrowsing.Builder(httpTransport, GOOGLE_JSON_FACTORY, null).setApplicationName(GOOGLE_APPLICATION_NAME)
        val safebrowsing = safebrowsingBuilder.build()
        val findThreatMatchesResponse =
            safebrowsing.threatMatches().find(findThreatMatchesRequest).setKey(GOOGLE_API_KEY).execute()

        val threatMatches = findThreatMatchesResponse.matches
        val threadList: MutableList<String> = mutableListOf()
        if (threatMatches != null && threatMatches.size > 0) {
            for (threatMatch in threatMatches) {
                val threatJsonString = threatMatch.get("threat").toString()
                val url = threatJsonString
                    .removePrefix("{\"url\":\"")
                    .removeSuffix("\"}")
                threadList.add(url)
            }
        }
        logger.info("URLs seguras: ${urls - threadList}")
        return threadList
    }
    /**
     * Crea una petición para encontrar amenazas en las URLs proporcionadas utilizando Google Safe Browsing.
     *
     * @param urls Lista de URLs a incluir en la petición.
     * @return Objeto [FindThreatMatchesRequest] configurado con la información de las URLs.
     */
    private fun createFindThreatMatchesRequest(urls: List<String>): FindThreatMatchesRequest {
        logger.info("Creando peticion a Google Safe Browsing")

        val findThreatMatchesRequest = FindThreatMatchesRequest()
        val clientInfo = ClientInfo()
        clientInfo.setClientId(GOOGLE_CLIENT_ID)
        clientInfo.setClientVersion(GOOGLE_CLIENT_VERSION)
        findThreatMatchesRequest.setClient(clientInfo)
        val threatInfo = ThreatInfo()
        threatInfo.setThreatTypes(GOOGLE_THREAT_TYPES)
        threatInfo.setPlatformTypes(GOOGLE_PLATFORM_TYPES)
        threatInfo.setThreatEntryTypes(GOOGLE_THREAT_ENTRYTYPES)
        val threatEntries: MutableList<ThreatEntry> = ArrayList()
        for (url in urls) {
            val threatEntry = ThreatEntry()
            threatEntry["url"] = url
            threatEntries.add(threatEntry)
        }
        threatInfo.setThreatEntries(threatEntries)
        findThreatMatchesRequest.setThreatInfo(threatInfo)

        logger.info("Peticion a Google Safe Browsing creada")
        return findThreatMatchesRequest
    }
}

