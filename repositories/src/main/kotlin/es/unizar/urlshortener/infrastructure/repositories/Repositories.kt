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

    /**
     * Finds a [ShortUrlEntity] by its hash value.
     *
     * @param hash The unique hash of the short URL.
     * @return The found [ShortUrlEntity], or null if no entity is found.
     */
    fun findByHash(hash: String): ShortUrlEntity?

    /**
     * Updates the 'safe' status of a [ShortUrlEntity] based on its target URL.
     *
     * @param target The target URL of the short URL.
     * @param safe The safety status to be set.
     */
    @Modifying
    @Transactional
    @Query("UPDATE ShortUrlEntity s SET s.safe = :safe WHERE s.target= :target")
    fun updateSafeStatusByTarget(target:  String, safe: Boolean)

    /**
     * Updates the QR code data for a [ShortUrlEntity] based on its hash value.
     *
     * @param hash The unique hash of the short URL.
     * @param qr The byte array representing the QR code image.
     */
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

    /**
     * Finds a [ClickEntity] by its associated short URL hash value.
     *
     * @param hash The hash value of the short URL.
     * @return The found [ClickEntity], or null if no entity is found.
     */
    fun findByHash(hash: String):  ClickEntity?

    /**
     * Finds all [ClickEntity] instances associated with a given short URL hash.
     *
     * @param hash The hash value of the short URL.
     * @return A list of [ClickEntity], or null if no entities are found.
     */
    fun findAllByHash(hash: String): List<ClickEntity>?


    /**
     * Updates the user agent information (browser and platform) for a [ClickEntity] based on the IP address.
     *
     * @param ip The IP address associated with the click.
     * @param browser The browser name to be updated.
     * @param platform The platform name to be updated.
     */
    @Modifying
    @Transactional
    @Query("UPDATE ClickEntity c SET c.browser = :browser, c.platform = :platform WHERE c.ip = :ip")
    fun updateUAByIp(ip: String, browser: String, platform: String)

    /**
     * Counts and groups clicks by browser for a given short URL hash.
     *
     * @param hash The hash value of the short URL.
     * @return A list of arrays, each containing a browser name and the corresponding click count.
     */
    @Query("SELECT c.browser, COUNT(c) FROM ClickEntity c WHERE c.hash = :hash GROUP BY c.browser")
    fun countClicksByBrowser(hash: String): List<Array<Any>>

    /**
     * Counts and groups clicks by platform for a given short URL hash.
     *
     * @param hash The hash value of the short URL.
     * @return A list of arrays, each containing a platform name and the corresponding click count.
     */
    @Query("SELECT c.platform, COUNT(c) FROM ClickEntity c WHERE c.hash = :hash GROUP BY c.platform")
    fun countClicksByPlatform(hash: String): List<Array<Any>>
}
