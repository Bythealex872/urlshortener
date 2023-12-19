package es.unizar.urlshortener.infrastructure.repositories

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Specification of the repository of [ShortUrlEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ShortUrlEntityRepository : JpaRepository<ShortUrlEntity, String> {
    fun findByHash(hash: String): ShortUrlEntity?
    
    @Modifying
    @Transactional
    @Query("UPDATE ShortUrlEntity s SET s.safe = :safe WHERE s.target= :target")
    fun updateSafeStatusByTarget(target:  String, safe: Boolean)

    @Modifying
    @Transactional
    @Query("UPDATE ShortUrlEntity s SET s.qr = :qr WHERE s.hash = :hash")
    fun updateQRCodeByHash(hash: String, qr: ByteArray)
}

/**
 * Specification of the repository of [ClickEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ClickEntityRepository : JpaRepository<ClickEntity, Long>{
    fun findByHash(hash: String):  ClickEntity?

    @Modifying
    @Transactional
    @Query("UPDATE ClickEntity c SET c.browser = :browser, c.platform = :platform WHERE c.ip = :ip")
    fun updateUAByIp(ip: String, browser: String, platform: String)

    @Query("SELECT c.browser, COUNT(c) FROM ClickEntity c WHERE c.hash = :hash GROUP BY c.browser")
    fun countClicksByBrowser(hash: String): List<Array<Any>>

    @Query("SELECT c.platform, COUNT(c) FROM ClickEntity c WHERE c.hash = :hash GROUP BY c.platform")
    fun countClicksByPlatform(hash: String): List<Array<Any>>
}
