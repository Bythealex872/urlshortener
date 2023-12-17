package es.unizar.urlshortener.core

import java.time.OffsetDateTime

/**
 * A [Click] captures a request of redirection of a [ShortUrl] identified by its [hash].
 */
data class Click(
    val hash: String,
    val properties: ClickProperties = ClickProperties(),
    val created: OffsetDateTime = OffsetDateTime.now()
)

/**
 * A [ShortUrl] is the mapping between a remote url identified by [redirection]
 * and a local short url identified by [hash].
 */
data class ShortUrl(
    val hash: String,
    val redirection: Redirection,
    val created: OffsetDateTime = OffsetDateTime.now(),
    val properties: ShortUrlProperties = ShortUrlProperties()
)

/**
 * A [Redirection] specifies the [target] and the [status code][mode] of a redirection.
 * By default, the [status code][mode] is 307 TEMPORARY REDIRECT.
 */
data class Redirection(
    val target: String,
    val mode: Int = 307
)

/**
 * A [ShortUrlProperties] is the bag of properties that a [ShortUrl] may have.
 */
data class ShortUrlProperties(
    val ip: String? = null,
    val sponsor: String? = null,
    val safe: Boolean? = null,
    val owner: String? = null,
    val country: String? = null,
    val qr: ByteArray? = null
)

/**
 * A [ClickProperties] is the bag of properties that a [Click] may have.
 */
data class ClickProperties(
    val ip: String? = null,
    val referrer: String? = null,
    val browser: String? = null,
    val platform: String? = null,
    val country: String? = null
)

/**
 * [CsvOutput] is a data class representing the output of a CSV processing operation.
 * It contains the original URI, the shortened URI, the QR code URL, an optional explanation message,
 * and the status of the URL processing.
 */
data class CsvOutput(
    val originalUri: String,
    val shortenedUri: String,
    val qr : String,
    val explanation: String = "",
    val status : String
)

/**
 * [UserAgent] holds information about the user agent from which a request was made.
 * It captures details about the browser type and the platform (operating system).
 */
data class UserAgent(
    val browser: String? = null,
    val platform: String? = null
)
