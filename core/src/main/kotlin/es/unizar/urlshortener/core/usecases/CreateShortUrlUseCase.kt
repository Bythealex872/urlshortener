@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, qrRequest: Boolean? = false, data: ShortUrlProperties): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService,
    private val qrRequestService: QRRequestService,
    private val safeBrowsingRequestService: SafeBrowsingRequestService,
    private val linkToService: LinkToService
) : CreateShortUrlUseCase {
    override fun create(url: String, qrRequest: Boolean?, data: ShortUrlProperties): ShortUrl {
        // Verificar si la URL es válida
        if (!validatorService.isValid(url)) {
            throw InvalidUrlException(url)
        }

        // Obtener el hash de la URL
        val id = hashService.hasUrl(url)

        if (qrRequest == true) {
            qrRequestService.sendQRMessage(Pair(id, linkToService.link(id).toString()))
        }

        // Buscar si ya existe una URL corta con el mismo objetivo y propiedades
        val existingShortUrl = shortUrlRepository.findByKey(id)
        if (existingShortUrl != null && existingShortUrl.redirection.target == url) {
            // Devuelve la URL corta existente si coincide con la URL objetivo
            return existingShortUrl
        }

        // Crear y guardar una nueva URL corta si no existe
        val newShortUrl = ShortUrl(
            hash = id,
            redirection = Redirection(target = url),
            properties = ShortUrlProperties(
                ip = data.ip,
                sponsor = data.sponsor,
            )
        )

        val shortUrl = shortUrlRepository.save(newShortUrl)

        safeBrowsingRequestService.sendSafeBrowsingMessage(Pair(id, url))

        return shortUrl
    }
}
