@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    private val logger: Logger = LoggerFactory.getLogger(CreateShortUrlUseCaseImpl::class.java)

    override fun create(url: String, qrRequest: Boolean?, data: ShortUrlProperties): ShortUrl {
        logger.info("Creando URL corta para $url")
        // Verificar si la URL es válida
        if (!validatorService.isValid(url)) {
            logger.error("URL inválida: $url")
            throw InvalidUrlException(url)
        }
        // Obtener el hash de la URL
        val id = hashService.hasUrl(url)

        if (qrRequest == true) {
            logger.info("Enviando mensaje para generar QR")
            qrRequestService.sendQRMessage(Pair(id, linkToService.link(id).toString()))
        }

        logger.info("Buscando si existe ya la $url en la BD")
        // Buscar si ya existe una URL corta con el mismo objetivo y propiedades
        val existingShortUrl = shortUrlRepository.findByKey(id)
        if (existingShortUrl != null && existingShortUrl.redirection.target == url) {
            // Devuelve la URL corta existente si coincide con la URL objetivo
            logger.info("URL corta existente: ${existingShortUrl.hash} -> ${existingShortUrl.redirection.target}")
            return existingShortUrl
        }

        logger.info("Creando URL corta")
        // Crear y guardar una nueva URL corta si no existe
        val newShortUrl = ShortUrl(
            hash = id,
            redirection = Redirection(target = url),
            properties = ShortUrlProperties(
                ip = data.ip,
                sponsor = data.sponsor,
            )
        )
        logger.info("Guardando URL corta: $id -> $url")
        val shortUrl = shortUrlRepository.save(newShortUrl)

        safeBrowsingRequestService.sendSafeBrowsingMessage(Pair(id, url))

        logger.info("URL corta creada: ${shortUrl.hash} -> ${shortUrl.redirection.target}")

        return shortUrl
    }
}
