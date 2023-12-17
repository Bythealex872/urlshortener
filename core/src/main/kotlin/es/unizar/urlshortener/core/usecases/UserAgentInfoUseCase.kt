@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import com.blueconic.browscap.Capabilities
import com.blueconic.browscap.UserAgentService
import es.unizar.urlshortener.core.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Given a key returns user agent information.
 */
interface UserAgentInfoUseCase {
    fun getUserAgentInfoByKey(key: String): Map<String, Any>
    fun returnUserAgentInfo(uaString: String?): UserAgent?

}

/**
 * Implementation of [UserAgentInfoUseCase].
 */
class UserAgentInfoUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val clickRepository: ClickRepositoryService
) : UserAgentInfoUseCase {
    private val parser = UserAgentService().loadParser()
    private val logger: Logger = LoggerFactory.getLogger(UserAgentInfoUseCaseImpl::class.java)
    // Método para obtener información del agente de usuario a partir de una clave
    // Parámetros:
    // - key: Clave única asociada a una URL corta
    // Valor de retorno:
    // - Map<String, Any>: Mapa que contiene información sobre el agente de usuario
    override fun getUserAgentInfoByKey(key: String): Map<String, Any>{
        logger.info("Buscando $key para obtener información del agente de usuario")
        val click = clickRepository.findByKey(key)
        val shortUrl = shortUrlRepository.findByKey(key)
        if(shortUrl == null){
            logger.error("No se ha encontrado la URL")
            throw RedirectionNotFound(key)
        }
        if(click == null){
            logger.error("No se han encontrado datos de la URL")
            throw RedirectionNotFound(key)
        }
        if(shortUrl.properties.safe == null){
            logger.error("No se ha validado la URL")
            throw RetryAfterException()
        }
        if(!shortUrl.properties.safe){
            logger.error("La URI recortada no es segura")
            throw RedirectionForbidden(key)
        }

        logger.info("Devolviendo información del agente de usuario de $key")
        return click.let {
            mapOf(
                "Hash" to it.hash,
                "Created" to it.created.toString(),
                "Properties" to it.properties,
            )
        }
    }
    /* Método para devolver información del agente de usuario a partir de una cadena de agente de usuario
    Parámetros:
     - uaString: Cadena que representa el agente de usuario
     Valor de retorno:
     - UserAgent: Objeto que contiene información sobre el agente de usuario (navegador y plataforma)*/
    override fun returnUserAgentInfo(uaString: String?): UserAgent {
        logger.info("Parseando el agente de usuario")
        val capabilities: Capabilities = parser.parse(uaString)
        val browser = capabilities.browser
        val platform = capabilities.platform
        logger.info("Devolviendo el agente de usuario con el navegador $browser y la plataforma $platform ")
        return UserAgent(browser, platform)
    }

}
