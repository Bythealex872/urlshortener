package es.unizar.urlshortener.core

import java.net.URI

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun findByKey(id: String): Click?
    fun save(cl: Click): Click

    fun updateUAByIp(ip: String, browser: String, platform: String)
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByKey(id: String): ShortUrl?
    fun save(su: ShortUrl): ShortUrl
    fun updateSafeStatusByHash(hash: String, safe: Boolean)
    fun updateQRCodeByHash(hash: String, qr: ByteArray)
}

/**
 * [ValidatorService] is the port to the service that validates if an url can be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface ValidatorService {
    fun isValid(url: String): Boolean
}

interface SafeBrowsingService {
    fun isSafe(url: String): Boolean
    fun urlsAreSafe(urls: List<String>): List<String>

}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface HashService {
    fun hasUrl(url: String): String
}

interface LinkToService {
    fun link(id: String) : URI
}

interface QRRequestService {
    fun sendQRMessage(p: Pair<String, String>)
}

interface UserAgentRequestService {
    fun sendUserAgentMessage(p: Triple<String, String, String?>)
}

interface SafeBrowsingRequestService {
    fun sendSafeBrowsingMessage(p: Pair<String, String>)
}
