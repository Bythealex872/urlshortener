package es.unizar.urlshortener.core.usecases

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.safebrowsing.Safebrowsing
import com.google.api.services.safebrowsing.model.ClientInfo
import com.google.api.services.safebrowsing.model.FindThreatMatchesRequest
import com.google.api.services.safebrowsing.model.ThreatEntry
import com.google.api.services.safebrowsing.model.ThreatInfo
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import java.util.*


/**
 * Verify if the url is safe with Google Safe Browsing.
 */
val GOOGLE_JSON_FACTORY: JacksonFactory = JacksonFactory.getDefaultInstance()
const val GOOGLE_API_KEY = "AIzaSyDIAUh3yuwjU8tfDhN-pLNQZze1CtIBQGI" // Google API key
const val GOOGLE_CLIENT_ID = "UrlShortener" // client id
const val GOOGLE_CLIENT_VERSION = "0.0.1" // client version
const val GOOGLE_APPLICATION_NAME = "UrlShortener" // appication name

val GOOGLE_THREAT_TYPES = listOf("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE",
    "POTENTIALLY_HARMFUL_APPLICATION")
val GOOGLE_PLATFORM_TYPES = listOf("ANY_PLATFORM")
val GOOGLE_THREAT_ENTRYTYPES = listOf("URL")
var httpTransport: NetHttpTransport? = null

interface SafeBrowsing {
    fun urlsAreSafe(urlList: List<String>) : List<String>
    fun changeStatusUrl(hash: String)
}
class SafeBrowsingImpl(
    private val shortUrlRepository: ShortUrlRepositoryService

) : SafeBrowsing {


    override fun urlsAreSafe(urlList: List<String>) : List<String> {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport()

        val findThreatMatchesRequest: FindThreatMatchesRequest = createFindThreatMatchesRequest(urlList)
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
        return threadList
    }

    override fun changeStatusUrl(hash: String) {
        shortUrlRepository.updateSafeStatusByHash(hash)
    }

    private fun createFindThreatMatchesRequest(urls: List<String>): FindThreatMatchesRequest {
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
        return findThreatMatchesRequest
    }

}

