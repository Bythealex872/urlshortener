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
import jakarta.websocket.server.ServerEndpoint
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
    fun qrCode(id: String, request: HttpServletRequest): ResponseEntity<Any>

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

data class ErrorResponse(
    val error: String
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
    val userAgentInfoUseCase: UserAgentInfoUseCase,
    val qrRequestService: QRRequestService
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
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor
            )
        ).let {
            logger.info("URL creada")
            val linkCreate = { hash: String -> linkTo<UrlShortenerControllerImpl> { redirectTo(hash, null) }.toUri() }

            val url = linkCreate(it.hash)

            if (data.qrRequest!!) {
                qrRequestService.requestQRcreation(Pair(it.hash, url.toString()))
            }
            val qr = if(data.qrRequest) "$url/qr" else ""
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
    override fun qrCode(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Any> {
        val qrCodeResource = ByteArrayResource(createQRCodeUseCase.getQRCode(id))
        logger.info("QR creado")

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
        try {
            val csvOutputList = mutableListOf<CsvOutput>()
            val csvParser = CSVParserBuilder().build()
            val csvReader = CSVReaderBuilder(file.inputStream.bufferedReader())
                .withCSVParser(csvParser)
                .build()

            val lines = csvReader.readAll()
            if (lines.any { it.size != 2}) {
                //return ResponseEntity(ErrorResponse("Error en el formato del archivo CSV"), HttpStatus.BAD_REQUEST)
            }
            for (line in lines) {
                if (line.size >= 2) {
                    csvOutputList.add(processCsvLine(line, request))
                } else {
                    logger.warn("Invalid CSV line: $line")
                }
            }

            val csvContent = createCSVUseCase.buildCsvContent(csvOutputList)
            val headers = HttpHeaders()
            headers.contentType = MediaType.parseMediaType("text/csv")
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.csv")
            val firstShortenedUrl = csvOutputList.firstOrNull()?.shortenedUri
            headers.set(HttpHeaders.LOCATION, firstShortenedUrl)

            return ResponseEntity(csvContent, headers, HttpStatus.CREATED)
        } catch (e: CsvException) {
            logger.error("Error processing CSV file", e)
            return ResponseEntity(ErrorResponse("Error en el formato del archivo CSV"), HttpStatus.BAD_REQUEST)
        } 
    }

    private fun processCsvLine(line: Array<String>, request: HttpServletRequest): CsvOutput {
        val uri = line[0].trim()
        val qrCodeIndicator = line[1].trim()
        val create = createShortUrlUseCase.create(
                url = uri,
                data = ShortUrlProperties(ip = request.remoteAddr)
        )
        val urlRecortada = linkTo<UrlShortenerControllerImpl> { redirectTo(create.hash, request) }.toUri()

        if (qrCodeIndicator == "1") {
            qrRequestService.requestQRcreation(Pair(create.hash, urlRecortada.toString()))
            return CsvOutput(uri, "$urlRecortada", "$urlRecortada/qr", "hola")
        } else {
            return CsvOutput(uri, "$urlRecortada", "no_qr", "hola")
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
    override fun returnUserAgentInfo(@PathVariable id: String): ResponseEntity<Map<String, Any>>
        = ResponseEntity.ok(userAgentInfoUseCase.getUserAgentInfoByKey(id));
}
interface BulkEndpoint {
    fun onOpen(session: Session)
    fun onClose(session: Session, closeReason: CloseReason)
    fun onMsg(message: String, session: Session)
    fun onError(session: Session, errorReason: Throwable)
}

@Component
@ServerEndpoint("/api/bulk-fast")
class Luis : BulkEndpoint {
    private val logger: Logger = LoggerFactory.getLogger(Luis::class.java)

    @OnOpen
    override fun onOpen(session: Session) {
        logger.info("Server Connected ... Session ${session.id}")
    }

    @OnClose
    override fun onClose(session: Session, closeReason: CloseReason) {
        logger.info("Session ${session.id} closed because of ${closeReason.reasonPhrase}")
    }

    @OnMessage
    override fun onMsg(message: String, session: Session) {
        logger.info("Server Message ... Session ${session.id}")
        logger.info("Message $message")

       val sendCsvBean = SpringContext.getBean(SendCSV::class.java)
        sendCsvBean.sendCSV(Pair("$message", session))
    }

    @OnError
    override fun onError(session: Session, errorReason: Throwable) {
        logger.error("Session ${session.id} closed because of ${errorReason.javaClass.name}")
    }
}
