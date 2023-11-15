package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.CsvOutput
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.usecases.CreateQRCodeUseCase
import es.unizar.urlshortener.core.usecases.CreateCSVUseCase
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
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

import com.blueconic.browscap.Capabilities;
import com.blueconic.browscap.UserAgentParser;
import com.blueconic.browscap.UserAgentService;

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

    fun processCsvFile(@RequestPart("file") file: MultipartFile,request: HttpServletRequest): ResponseEntity<String>/**
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
 * Structure of the user agent.
 */
data class UserAgent(
    val browser: String,
    val platform: String,
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
    val createQRCodeUseCase: CreateQRCodeUseCase,
    val createCSVUseCase : CreateCSVUseCase
) : UrlShortenerController {

    //Variables privadas 
    private val userAgentMap: MutableMap<String, UserAgent> = mutableMapOf()
    private val parser = UserAgentService().loadParser()

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))

            // Añadimos datos de userAgent
            val userAgentString = request.getHeader("User-Agent") ?: ""
            val capabilities: Capabilities = parser.parse(userAgentString)

            // Almacena la información en el mapa
            userAgentMap[id] = UserAgent(
                    browser = capabilities.browser,
                    platform = capabilities.platform
            )

            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Unit>(h, HttpStatus.valueOf(it.mode))
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
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val qr = if(data.qrRequest!!) "$url/qr" else ""
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
        val url = linkTo<UrlShortenerControllerImpl> { redirectTo(id, request) }.toUri().toString()
        val qrCodeImage = createQRCodeUseCase.createQRCode(url)
        
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
    override fun processCsvFile(@RequestPart("file") file: MultipartFile,
    request: HttpServletRequest): ResponseEntity<String> {
        val csvOutputList = mutableListOf<CsvOutput>()

        // Process CSV file
        val lines = file.inputStream.bufferedReader().readLines()
        for (line in lines) {
            val uri = line.trim()
           val create =  createShortUrlUseCase.create(
                url = uri,
                data = ShortUrlProperties(
                    ip = request.remoteAddr,
                )
            )
            val urlrecortada = linkTo<UrlShortenerControllerImpl> { redirectTo(create.hash, request) }.toUri()
            csvOutputList.add(CsvOutput(uri, "$urlrecortada" ,":)"))
        }

        val csvContent = createCSVUseCase.buildCsvContent(csvOutputList)
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType("text/csv")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.csv")

        return ResponseEntity(csvContent, headers, HttpStatus.CREATED)
    }
    /* 

    private fun shortenUri(uri: String): Pair<String, String> {
    // Analiza la URI proporcionada para obtener sus componentes
        val parsedUri = URI(uri)

        // Usa una estructura de control 'when' para manejar diferentes casos basados en el esquema de la URI
        return when (parsedUri.scheme) {
        // Si el esquema es 'http' o 'https', procesa la URI
            "http", "https" -> {
                // Simula el acortamiento de la URI. Aquí, simplemente se concatena un hash de la URI
                // con una base de URL predefinida para crear una 'URI acortada'
                val shortened = "http://short.uri/" + uri.hashCode()

                // Devuelve un par (Pair) con la URI acortada y una cadena vacía para el mensaje de error
                shortened to ""
            }
            // Para cualquier otro esquema (o si no hay esquema)
            else -> {
                // Devuelve un par con una cadena vacía para la URI acortada y un mensaje de error
                "" to "debe ser una URI http o https"
            }
        }
    }
    */

    @GetMapping("/api/link/{id}")
    override fun returnUserAgentInfo(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        val userAgentInfo = userAgentMap[id]

        return if (userAgentInfo != null) {
            // Construir el resumen acumulado
            val summary = mapOf(
                "id" to id,
                "browser" to userAgentInfo.browser,
                "platform" to userAgentInfo.platform,
            )
            ResponseEntity.ok(summary)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
}
