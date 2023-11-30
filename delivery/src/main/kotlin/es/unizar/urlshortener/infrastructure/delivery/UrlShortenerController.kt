@file:Suppress("WildcardImport")

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
import es.unizar.urlshortener.core.*
import com.opencsv.*
import com.opencsv.exceptions.CsvException
import com.opencsv.exceptions.CsvValidationException
import es.unizar.urlshortener.core.usecases.*
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

    fun processCsvFile(@RequestPart("file") file: MultipartFile,request: HttpServletRequest): ResponseEntity<String>
    
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
    val createQRCodeUseCase: CreateQRCodeUseCase,
    val createCSVUseCase : CreateCSVUseCase,
    val userAgentInfoUseCase: UserAgentInfoUseCase

) : UrlShortenerController {

    //Variables privadas 
    private val parser = UserAgentService().loadParser()
    private val logger: Logger = LoggerFactory.getLogger(UrlShortenerControllerImpl::class.java)

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        //Verifica si el id es un hash válido
        if(redirectUseCase.redirectTo(id).mode == 404){
            logger.error("Error 404: No se ha encontrado el hash")
            return ResponseEntity.notFound().build()
        }

        val userAgentString = request.getHeader("User-Agent")
        // Verifica si hay User-Agent presente antes de intentar analizarlo
        if (!userAgentString.isNullOrBlank()) {
            val capabilities: Capabilities = parser.parse(userAgentString)
            val browser = capabilities.browser
            val platform = capabilities.platform

            // LogClick con información del User-Agent
            logClickUseCase.logClick(id, ClickProperties(
                    ip = request.remoteAddr,
                    browser = browser,
                    platform = platform
            ))
        } else {
            // LogClick solo con la información de la IP (sin User-Agent)
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
        }
        
        logger.info("Redirección creada creada")
        val redirectResult = redirectUseCase.redirectTo(id)
        val headers = HttpHeaders()
        headers.location = URI.create(redirectResult.target)
        return ResponseEntity<Unit>(headers, HttpStatus.valueOf(redirectResult.mode))
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
        val url = linkTo<UrlShortenerControllerImpl> { redirectTo(id, request) }.toUri().toString()
        val qrCodeBytes = createQRCodeUseCase.createQRCode(url)
        val qrCodeResource = ByteArrayResource(qrCodeBytes)
        
        logger.info("QR creado")

        val h = HttpHeaders().apply {
            contentType = MediaType.IMAGE_PNG
            cacheControl = "no-cache, no-store, must-revalidate"
            pragma = "no-cache"
            expires = 0
        }

        return ResponseEntity<ByteArrayResource>(qrCodeResource, h, HttpStatus.OK)
    }

    @PostMapping("/api/bulk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun processCsvFile(
        @RequestPart("file") file: MultipartFile,
        request: HttpServletRequest
    ): ResponseEntity<String> {
        try {
            val csvOutputList = mutableListOf<CsvOutput>()
            val csvParser = CSVParserBuilder().build()
            val csvReader = CSVReaderBuilder(file.inputStream.bufferedReader())
                .withCSVParser(csvParser)
                .build()

            val lines = csvReader.readAll()
            for (line in lines) {
                for (uri in line) {
                    val trimmedUri = uri.trim()
                    val create = createShortUrlUseCase.create(
                        url = trimmedUri,
                        data = ShortUrlProperties(
                            ip = request.remoteAddr,
                        )
                    )
                    val urlRecortada =
                        linkTo<UrlShortenerControllerImpl> { redirectTo(create.hash, request) }.toUri()
                    csvOutputList.add(CsvOutput(trimmedUri, "$urlRecortada", ":)"))
                }
            }

            val csvContent = createCSVUseCase.buildCsvContent(csvOutputList)
            logger.info("CSV creado")
            val headers = HttpHeaders()
            headers.contentType = MediaType.parseMediaType("text/csv")
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.csv")

            return ResponseEntity(csvContent, headers, HttpStatus.CREATED)
        } catch (e: CsvException) {
            // Handle CSV validation exception
            logger.error("Error processing CSV file", e)
            return ResponseEntity("Error en el formato del archivo CSV", HttpStatus.BAD_REQUEST)
        } 
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
        val userAgentInfo = userAgentInfoUseCase.getUserAgentInfoByKey(id)

        return if (userAgentInfo != null) {
            ResponseEntity.ok(userAgentInfo)
        } else {
            ResponseEntity.notFound().build()
        }
    }

}
