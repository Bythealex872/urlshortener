package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*


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
            throw RetryAfterException()
        }
        if(shortUrl.redirection == null){
            throw RedirectionForbiden(key)
        }

        logClickUseCase.logClick(key, ip, UA)

        // Devuelve la lógica de redirección
        return shortUrl.redirection
    }
}



