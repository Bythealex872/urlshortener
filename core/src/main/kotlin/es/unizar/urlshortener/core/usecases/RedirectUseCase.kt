package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

private const val CORRECTO = 307

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

    override fun redirectTo(key: String, ip: String, ua: String?): Redirection {
        println("RedirectUseCaseImpl: redirectTo: key: $key, ip: $ip, UA: $ua")
        val shortUrl = shortUrlRepository.findByKey(key) ?: throw RedirectionNotFound(key)

        // Verifica si la URI recortada no existe
        if(!shortUrl.properties.safe!!){ // no valida, posible spam
            throw RetryAfterException()
        }
        if(shortUrl.redirection.mode != CORRECTO){ // no operativa
            throw RedirectionForbidden(key)
        }

        uaService.sendUserAgentMessage(Triple(key, ip, ua))
        //logClickUseCase.logClick(key, ip, ua)

        // Devuelve la lógica de redirección
        return shortUrl.redirection
    }
}



