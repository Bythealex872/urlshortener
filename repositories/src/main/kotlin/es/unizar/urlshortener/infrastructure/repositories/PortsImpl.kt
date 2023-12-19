package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService


/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    /**
     * Finds a click by its unique identifier.
     *
     * @param id The identifier of the click.
     * @return The found [Click] or null if no click is found.
     */
    override fun findByKey(id: String): Click? = clickEntityRepository.findByHash(id)?.toDomain()

    /**
     * Finds all clicks associated with a specific identifier.
     *
     * @param id The identifier of the click.
     * @return A list of [Click], or null if no clicks are found.
     */
    override fun findAllByKey(id: String): List<Click>? = clickEntityRepository.findAllByHash(id)?.map { it.toDomain() }

    /**
     * Saves a click entity to the repository.
     *
     * @param cl The [Click] to be saved.
     * @return The saved [Click] entity.
     */
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()

    /**
     * Updates user agent information by IP address.
     *
     * @param ip The IP address associated with the click.
     * @param browser The browser name.
     * @param platform The platform name.
     */
    override fun updateUAByIp(ip: String, browser: String, platform: String) =
            clickEntityRepository.updateUAByIp(ip, browser, platform)

    /**
     * Retrieves click statistics by browser for a specific identifier.
     *
     * @param id The identifier.
     * @return A map of browser names to their respective click counts.
     */
    override fun getClickStatsByBrowser(id: String): Map<String, Long> =
         clickEntityRepository.countClicksByBrowser(id).associate { it[0] as String to (it[1] as Long) }

    /**
     * Retrieves click statistics by platform for a specific identifier.
     *
     * @param id The identifier.
     * @return A map of platform names to their respective click counts.
     */
    override fun getClickStatsByPlatform(id: String): Map<String, Long> =
         clickEntityRepository.countClicksByPlatform(id).associate { it[0] as String to (it[1] as Long) }
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {

    /**
     * Finds a short URL by its unique identifier.
     *
     * @param id The identifier of the short URL.
     * @return The found [ShortUrl] or null if no short URL is found.
     */
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    /**
     * Saves a short URL entity to the repository.
     *
     * @param su The [ShortUrl] to be saved.
     * @return The saved [ShortUrl] entity.
     */
    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()

    /**
     * Updates the safety status of a short URL based on its target.
     *
     * @param target The target URL of the short URL.
     * @param safe The safety status to be set.
     */
    override fun updateSafeStatusByTarget(target: String, safe: Boolean) =
            shortUrlEntityRepository.updateSafeStatusByTarget(target, safe)

    /**
     * Updates the QR code data for a short URL based on its hash value.
     *
     * @param hash The unique hash of the short URL.
     * @param qr The byte array representing the QR code image.
     */
    override fun updateQRCodeByHash(hash: String, qr: ByteArray) =
            shortUrlEntityRepository.updateQRCodeByHash(hash, qr)
}

