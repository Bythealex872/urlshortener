package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.CsvOutput
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.usecases.CreateQRCodeUseCase
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
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream
import org.springframework.core.io.ByteArrayResource
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart


/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Unit>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    /**
     * Returns a QR code with the url identified by its [id].
     *
     * **Note**: Delivery of use cases [CreateQRCodeUseCase].
     */
    fun qrCode(id: String, request: HttpServletRequest): ResponseEntity<ByteArrayResource>

    fun processCsvFile(@RequestPart("file") file: MultipartFile): ResponseEntity<String>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val qrRequest: Boolean? = null
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
    val createQRCodeUseCase: CreateQRCodeUseCase
) : UrlShortenerController {

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Unit>(h, HttpStatus.valueOf(it.mode))
        }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                qr = data.qrRequest.toString()
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val qr = data.qrRequest?.let { "$url/qr" } ?: ""
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe,
                    "qr" to qr
                )
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @GetMapping("/{id}/qr", produces = [MediaType.IMAGE_PNG_VALUE])
    override fun qrCode(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<ByteArrayResource> {
        // Llamamos a la función para generar la imagen del código QR
        val qrCodeImage = createQRCodeUseCase.createQRCode(id)
        
        // Convertimos la imagen a un array de bytes
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(qrCodeImage, "PNG", outputStream)
        val qrCodeBytes = outputStream.toByteArray()
        val qrCodeResource = ByteArrayResource(qrCodeBytes)

        // Preparamos los encabezados de la respuesta para indicar que es una imagen
        val h = HttpHeaders()
        h.contentType = MediaType.IMAGE_PNG
        h.cacheControl = "no-cache, no-store, must-revalidate"
        h.pragma = "no-cache"
        h.expires = 0

        return ResponseEntity<ByteArrayResource>(qrCodeResource, h, HttpStatus.OK)
    }

    @PostMapping("/api/bulk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun processCsvFile(@RequestPart("file") file: MultipartFile): ResponseEntity<String> {
        val csvOutputList = mutableListOf<CsvOutput>()

        // Process CSV file
        val lines = file.inputStream.bufferedReader().readLines()
        for (line in lines) {
          
                val uri = line.trim()
                // Perform logic to shorten the URI and get additional information if needed
                val shortenedUri = shortenUri(uri)
                val explanation = "" // Explanation for cases where URI cannot be shortened, you can set it as needed
                csvOutputList.add(CsvOutput(uri, shortenedUri, explanation))
            
            
        }

        val csvContent = buildCsvContent(csvOutputList)
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType("text/csv")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.csv")

        return ResponseEntity(csvContent, headers, HttpStatus.CREATED)
    }

    private fun shortenUri(uri: String): String {
        // Implement logic to shorten the URI, return shortened URI or throw an exception if unable to shorten
        return uri // Placeholder logic, implement according to your requirements
    }

    private fun buildCsvContent(outputList: List<CsvOutput>): String {
        val csvContent = StringBuilder()
        csvContent.append("Original URI,Shortened URI,Explanation")
        csvContent.append("\n")

        for (output in outputList) {
            csvContent.append("${output.originalUri},${output.shortenedUri},${output.explanation}")
            csvContent.append("\n")
        }

        return csvContent.toString()
    }

}
