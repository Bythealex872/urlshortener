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
    override fun findByKey(id: String): Click? = clickEntityRepository.findByHash(id)?.toDomain()
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()

    override fun updateUAByIp(ip: String, browser: String, platform: String) =
            clickEntityRepository.updateUAByIp(ip, browser, platform)
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()
    override fun updateSafeStatusByHash(hash: String, safe: Boolean) =
            shortUrlEntityRepository.updateSafeStatusByHash(hash, safe)
    override fun updateQRCodeByHash(hash: String, qr: ByteArray) =
            shortUrlEntityRepository.updateQRCodeByHash(hash, qr)
}

