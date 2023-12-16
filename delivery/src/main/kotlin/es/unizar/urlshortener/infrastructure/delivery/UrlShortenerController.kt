@file:Suppress("WildcardImport", "LongParameterList")

package es.unizar.urlshortener.infrastructure.delivery

import jakarta.servlet.http.HttpServletRequest
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import org.springframework.core.io.ByteArrayResource
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.bind.annotation.RequestPart
import es.unizar.urlshortener.core.*
import com.opencsv.*
import com.opencsv.exceptions.CsvException
import es.unizar.urlshortener.core.usecases.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import jakarta.websocket.*
import jakarta.websocket.server.HandshakeRequest
import jakarta.websocket.server.ServerEndpoint
import jakarta.websocket.server.ServerEndpointConfig
import org.springframework.stereotype.Component


/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest?): ResponseEntity<Unit>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    /**
     * Returns a QR code with the url identified by its [id].
     *
     * **Note**: Delivery of use cases [QRCodeUseCase].
     */
    fun qrCode(id: String, request: HttpServletRequest): ResponseEntity<ByteArrayResource>

    fun processCsvFile(@RequestPart("file") file: MultipartFile,request: HttpServletRequest): ResponseEntity<Any>
    
    /**
    * Returns the user agent information from the map.
    */
    fun returnUserAgentInfo(@PathVariable id: String): ResponseEntity<Map<String, Any>>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val qrRequest: Boolean? = false
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)





/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val createQRCodeUseCase: QRCodeUseCase,
    val createCSVUseCase : CreateCSVUseCase,
    val userAgentInfoUseCase: UserAgentInfoUseCase
) : UrlShortenerController {

    private val logger: Logger = LoggerFactory.getLogger(UrlShortenerControllerImpl::class.java)

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest?): ResponseEntity<Unit> =
    redirectUseCase.redirectTo(id, request!!.remoteAddr, request.getHeader("User-Agent")).let {
        logger.info("Redirección creada creada ${request.remoteAddr}")
        val headers = HttpHeaders()
        headers.location = URI.create(it.target)
        ResponseEntity<Unit>(headers, HttpStatus.valueOf(it.mode))
    }



    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            qrRequest = data.qrRequest,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor
            )
        ).let {
            logger.info("URL creada")
            val linkCreate = { hash: String -> linkTo<UrlShortenerControllerImpl> { redirectTo(hash, null) }.toUri() }

            val url = linkCreate(it.hash)
            val qr = if(data.qrRequest!!) "$url/qr" else ""
            val h = HttpHeaders().apply {
                location = url
            }
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe,
                    "qr" to qr
                )
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @GetMapping("/{id}/qr")
    override fun qrCode(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<ByteArrayResource> {
        val qrCodeResource = ByteArrayResource(createQRCodeUseCase.getQRCode(id))
        logger.info("QR pedido")

        val headers = HttpHeaders().apply {
            contentType = MediaType.IMAGE_PNG
            cacheControl = "no-cache, no-store, must-revalidate"
            pragma = "no-cache"
            expires = 0
        }

        return ResponseEntity(qrCodeResource, headers, HttpStatus.OK)
    }

    @PostMapping("/api/bulk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun processCsvFile(
        @RequestPart("file") file: MultipartFile,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        val inputStream = file.inputStream
        val (csvContent, firstShortenedUri) = createCSVUseCase.processAndBuildCsv(inputStream, request.remoteAddr)
        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType("text/csv")
            set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.csv")
            firstShortenedUri?.let {
                location = URI.create(it)
            }
        }

        // Si el contenido del CSV está vacío (es decir, el archivo estaba vacío), devuelve un mensaje adecuado
        return if (csvContent.trim().isEmpty()) {
            ResponseEntity("El archivo CSV está vacío.", HttpStatus.OK)
        } else {
            ResponseEntity(csvContent, headers, HttpStatus.CREATED)
        }
    }

    @GetMapping("/api/link/{id}")
    override fun returnUserAgentInfo(@PathVariable id: String): ResponseEntity<Map<String, Any>>
        = ResponseEntity.ok(userAgentInfoUseCase.getUserAgentInfoByKey(id))
}
