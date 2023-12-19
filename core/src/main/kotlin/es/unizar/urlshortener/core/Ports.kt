package es.unizar.urlshortener.core

import java.net.URI

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun findByKey(id: String): Click?
    fun findAllByKey(id: String): List<Click>?
    fun save(cl: Click): Click

    fun updateUAByIp(ip: String, browser: String, platform: String)

    fun getClickStatsByBrowser(id: String): Map<String, Long>
  
    fun getClickStatsByPlatform(id: String): Map<String, Long>

}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByKey(id: String): ShortUrl?
    fun save(su: ShortUrl): ShortUrl
    fun updateSafeStatusByTarget(target: String, safe: Boolean)
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

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface HashService {
    fun hasUrl(url: String): String
}

/**
 * [SafeBrowsingService] is the port for a service that determines the safety status of URLs.
 * It can check individual URLs or batches of URLs to assess whether they are deemed safe.
 */
interface SafeBrowsingService {
    fun isSafe(url: String): Boolean
    fun urlsAreSafe(urls: List<String>): List<String>
}

/**
 * [LinkToService] provides a mechanism to create a URI based on a given identifier.
 */
interface LinkToService {
    fun link(id: String): URI
}

/**
 * [QRRequestService] is responsible for handling QR code generation requests.
 * It takes a pair consisting of the identifier and the URL for which the QR code should be generated.
 */
interface QRRequestService {
    fun sendQRMessage(p: Pair<String, String>)
}

/**
 * [UserAgentRequestService] is responsible for handling requests related to user agent information.
 * It takes a triple consisting of the identifier, IP address, and optionally the previous user agent string.
 */
interface UserAgentRequestService {
    fun sendUserAgentMessage(p: Triple<String, String, String?>)
}

/**
 * [SafeBrowsingRequestService] handles requests for safe browsing checks.
 * It takes a pair of an identifier and a URL and initiates a process to determine if the URL is safe.
 */
interface SafeBrowsingRequestService {
    fun sendSafeBrowsingMessage(p: Pair<String, String>)
}
