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

    fun redirectTo(key: String, ip: String, ua: String?): Redirection

}
/**
 * Implementation of [RedirectUseCase].
 */
class RedirectUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService,
        private val uaService: UserAgentRequestService
) : RedirectUseCase {
    private val logger: Logger = LoggerFactory.getLogger(RedirectUseCaseImpl::class.java)

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

        uaService.sendUserAgentMessage(Triple(key, ip, ua))
        logger.info("Redirecting to ${shortUrl.redirection}")
        // Devuelve la lógica de redirección
        return shortUrl.redirection
    }
}



