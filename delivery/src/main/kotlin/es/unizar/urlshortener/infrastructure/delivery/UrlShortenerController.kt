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
import es.unizar.urlshortener.core.usecases.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory


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

    fun processCsvFile(@RequestPart("file") file: MultipartFile,request: HttpServletRequest): ResponseEntity<String>

    /**
    * Returns the user agent information from the [map].
    */
    fun returnUserAgentInfo(@PathVariable id: String): ResponseEntity<Map<String, Any>>
}

@Schema(description = "Data required to create a short url")
data class ShortUrlDataIn(
        @Schema(description = "The URL to shorten", required = true)
        val url: String,

        @Schema(description = "An optional sponsor for the URL")
        val sponsor: String? = null,

        @Schema(description = "Flag to indicate if a QR code is requested")
        val qrRequest: Boolean? = false
)

@Schema(description = "Data returned after the creation of a short URL")
data class ShortUrlDataOut(
        @Schema(description = "The shortened URL")
        val url: URI? = null,

        @Schema(description = "Additional properties of the shortened URL")
        val properties: Map<String, Any> = emptyMap()
)


@Tag(name = "UrlShortenerController", description = "The controller for URL shortening operations")
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val createQRCodeUseCase: QRCodeUseCase,
    val createCSVUseCase : CreateCSVUseCase,
    val userAgentInfoUseCase: UserAgentInfoUseCase
) : UrlShortenerController {

    private val logger: Logger = LoggerFactory.getLogger(UrlShortenerControllerImpl::class.java)

    @Operation(
        summary = "Redirect to a URL",
        description = "Redirects to the full URL corresponding to the given short URL identifier",
        responses = [
            ApiResponse(
                responseCode = "307",
                description = "Redirection successful"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ID not found",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorMessage::class)
                )]
            ),
            ApiResponse(
                responseCode = "403",
                description = "URL is not safe",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorMessage::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "URL has not been validated yet",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorMessage::class)
                )]
            )
        ]
    )
    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest?): ResponseEntity<Unit> =
    redirectUseCase.redirectTo(id, request!!.remoteAddr, request.getHeader("User-Agent")).let {
        logger.info("Redirección creada creada ${request.remoteAddr}")
        val headers = HttpHeaders()
        headers.location = URI.create(it.target)
        ResponseEntity<Unit>(headers, HttpStatus.valueOf(it.mode))
    }

    @Operation(
        summary = "Create a short URL",
        description = "Creates a short URL from the provided long URL and other details",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "Short URL created",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ShortUrlDataOut::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "URL does not support a valid schema",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorMessage::class)
                )]
            )
        ]
    )
    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(
        @RequestBody(
            description = "Data for creating a short URL",
            required = true,
            content = [Content(
                mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                schema = Schema(implementation = ShortUrlDataIn::class)
            )]
        )
        data: ShortUrlDataIn,
        request: HttpServletRequest
    ): ResponseEntity<ShortUrlDataOut> =
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
            val safe = if (it.properties.safe == null) "URI de destino no validada todavía" else "${it.properties.safe}"
            val qr = if(data.qrRequest!!) "$url/qr" else ""
            val h = HttpHeaders().apply {
                location = url
            }
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to safe,
                    "qr" to qr
                )
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @Operation(
        summary = "Deliver a QR code",
        description = "Delivers the QR code associated with the ID",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "QR code delivered",
                content = [Content(
                    mediaType = MediaType.IMAGE_PNG_VALUE,
                    schema = Schema(implementation = ByteArrayResource::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "ID not found",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorMessage::class)
                )]
            ),
            ApiResponse(
                responseCode = "403",
                description = "URL is not safe",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorMessage::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "URL has not been validated yet",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorMessage::class)
                )]
            )
        ]
    )
    @GetMapping("/{id}/qr")
    override fun qrCode(
        @Parameter(description = "Identifier of the short URL", required = true)
        @PathVariable id: String,
        request: HttpServletRequest
    ): ResponseEntity<ByteArrayResource> {
        val qrCodeResource = ByteArrayResource(createQRCodeUseCase.getQRCode(id))
        logger.info("QR pedido")

        val headers = HttpHeaders().apply {
            contentType = MediaType.IMAGE_PNG
            cacheControl = "no-cache, no-store, must-revalidate"
            pragma = "no-cache"
            expires = 0
        }

        return ResponseEntity<ByteArrayResource>(qrCodeResource, headers, HttpStatus.OK)
    }

    @Operation(
        summary = "Create a CSV",
        description = "Creates a CSV with the original URL, shorted URL, QR path, error message" +
                "and the validation state",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "CSV created",
                content = [Content(
                    mediaType = "text/csv",
                    schema = Schema(implementation = ByteArrayResource::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "The CSV could not be processed",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorMessage::class)
                )]
            )
        ]
    )
    @PostMapping("/api/bulk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun processCsvFile(
        @RequestPart("file") file: MultipartFile,
        request: HttpServletRequest
    ): ResponseEntity<String> {
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

    @Operation(
        summary = "Return a summary of the User Agent information",
        description = "Returns a summary of the User Agent information associated with the given ID",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User Agent resume delivered",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "ID not found",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorMessage::class)
                )]
            ),
            ApiResponse(
                responseCode = "403",
                description = "URL is not safe",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorMessage::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "URL has not been validated yet",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorMessage::class)
                )]
            )
        ]
    )
    @GetMapping("/api/link/{id}")
    override fun returnUserAgentInfo(
        @Parameter(description = "Identifier of the short URL", required = true)
        @PathVariable id: String
    ): ResponseEntity<Map<String, Any>>
        = ResponseEntity.ok(userAgentInfoUseCase.getUserAgentInfoByKey(id))
}
