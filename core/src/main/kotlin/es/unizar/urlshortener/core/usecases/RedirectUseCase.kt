package es.unizar.urlshortener.core.usecases

import com.blueconic.browscap.Capabilities
import com.blueconic.browscap.UserAgentService
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 * **Note**: This is an example of functionality.
 */
interface RedirectUseCase {
    fun redirectTo(key: String, ip: String, UA: String): Redirection

}
/**
 * Implementation of [RedirectUseCase].
 */
class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val logClickUseCase: LogClickUseCase

) : RedirectUseCase {

    override fun redirectTo(key: String, ip: String, UA: String): Redirection {
        val shortUrl = shortUrlRepository.findByKey(key)

        // Verifica si la URI recortada no existe
        if (shortUrl == null) {
            throw RedirectionNotFound(key)
        }
        if(!shortUrl.properties.safe){
            // Cambiar throw
            throw RedirectionNotFound(key)
        }

        logClickUseCase.logClick(key, ip, UA)

        // Devuelve la lógica de redirección
        return shortUrl.redirection
    }
}



