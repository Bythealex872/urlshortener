@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 * **Note**: This is an example of functionality.
 */
interface RedirectUseCase {

    /** Método para redireccionar a una URL corta dada su clave
    * @param key: Clave única asociada a una URL corta
    * @param ip: Dirección IP del usuario que realiza la redirección
    * @param ua: Agente de usuario (User-Agent) del navegador del usuario, puede ser nulo
    * @return Redirection: Objeto que contiene la URL objetivo y el modo de redirección HTTP
    */
    fun redirectTo(key: String, ip: String, ua: String?): Redirection

}
/**
 * Implementation of [RedirectUseCase].
 */
class RedirectUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService,
        private val uaRequestService: UserAgentRequestService
) : RedirectUseCase {
    private val logger: Logger = LoggerFactory.getLogger(RedirectUseCaseImpl::class.java)

    /** Implementación del método para redireccionar a una URL corta
    * @param key: Clave única asociada a una URL corta
    * @param ip: Dirección IP del usuario que realiza la redirección
    * @param ua: Agente de usuario (User-Agent) del navegador del usuario, puede ser nulo
    * @return Redirection: Objeto que contiene la URL objetivo y el modo de redirección HTTP
    */
    override fun redirectTo(key: String, ip: String, ua: String?): Redirection {
        logger.info("Buscando $key para redireccionar")
        val shortUrl = shortUrlRepository.findByKey(key)
        if(shortUrl == null){
            logger.error("No se ha encontrado la URI recortada")
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

        uaRequestService.sendUserAgentMessage(Triple(key, ip, ua))
        logger.info("Redirecting to ${shortUrl.redirection}")
        // Devuelve la lógica de redirección
        return shortUrl.redirection
    }
}



