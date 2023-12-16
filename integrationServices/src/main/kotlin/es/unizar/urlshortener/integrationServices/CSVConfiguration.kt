package es.unizar.urlshortener.integrationServices

import es.unizar.urlshortener.core.LinkToService
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCaseImpl

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.ExecutorChannel
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.integrationFlow
import org.springframework.messaging.MessageChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.scheduling.annotation.EnableScheduling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
//import jakarta.websocket.*
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor


/* 
override fun redirectTo(@PathVariable id: String): ResponseEntity<Unit> {
    //Verifica si el id es un hash válido
    if(redirectUseCase.redirectTo(id).mode == 404){
        logger.error("Error 404: No se ha encontrado el hash")
        return ResponseEntity.notFound().build()
    }
    logger.info("Redirección creada creada")
    val redirectResult = redirectUseCase.redirectTo(id)
    val headers = HttpHeaders()
    headers.location = URI.create(redirectResult.target)
    return ResponseEntity<Unit>(headers, HttpStatus.valueOf(redirectResult.mode))
}*/



@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(myHandler(), "/api/fast-bulk")
            .addInterceptors(HttpSessionHandshakeInterceptor())
            .setAllowedOrigins("*")
    }

    fun myHandler(): MyWebSocketHandler {
        return MyWebSocketHandler()
    }
}

class MyWebSocketHandler : TextWebSocketHandler() {

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        println("Received message: ${message.payload}")
        session.sendMessage(TextMessage("Hello from server"))
        val sendCsvBean = SpringContext.getBean(CSVRequestGateway::class.java)
        val laddr = session.localAddress
        sendCsvBean.sendCSVMessage(Pair(message.payload, session))
    }
    override fun afterConnectionEstablished(session: WebSocketSession) {
        val localAddress = session.attributes["localAddress"]
        println("WebSocket session established from local address: $localAddress")
    }

}

class MyHandshakeInterceptor : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        // Capture local address from the request
        val localAddress = request.remoteAddress?.address?.hostAddress
        println("Local Address: $localAddress")

        // You can add attributes to be used in the WebSocket session
        attributes["localAddress"] = localAddress ?: "unknown"
        attributes["port"] = request.remoteAddress?.port ?: "unknown"
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        // Post-handshake logic
    }
}
@Configuration
@EnableIntegration
@EnableScheduling
class CSVCodeIntegrationConfiguration(
        private val linkToService: LinkToService,

) {

    companion object {
        private const val CSV_CREATION_CORE_POOL_SIZE = 2
        private const val CSV_CREATION_MAX_POOL_SIZE = 4
        private const val CSV_CREATION_QUEUE_CAPACITY = 25
        private const val CSV_CREATION_THREAD_NAME = "csv-update-"
        private val creationLock = Object()
    }

    private val logger: Logger = LoggerFactory.getLogger(CSVCodeIntegrationConfiguration::class.java)
    private val creationLock = Object()
    fun csvCreationExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = CSV_CREATION_CORE_POOL_SIZE
        maxPoolSize = CSV_CREATION_MAX_POOL_SIZE
        setQueueCapacity(CSV_CREATION_QUEUE_CAPACITY)
        setThreadNamePrefix(CSV_CREATION_THREAD_NAME)
        initialize()
    }

    @Bean
    fun csvCreationChannel(): MessageChannel = ExecutorChannel(csvCreationExecutor())

    @Bean
    fun csvFlow(createShortUrlUseCase: CreateShortUrlUseCaseImpl): IntegrationFlow = integrationFlow(csvCreationChannel()) {
        handle<Pair<String, WebSocketSession>> { payload, _ ->
            val (uri, session) = payload
            logger.info("Procesando URI: $uri")
            val delimiter = detectDelimiter(uri)
            val parts = uri.split(delimiter)
            val trimmedUri = parts[0].trim()
            val qrRequest = parts[1].trim() == "1"
            logger.info("QR request: $qrRequest")
            var error = "no_error"
            lateinit var create: es.unizar.urlshortener.core.ShortUrl
            // Crear URL corta
            try {
                 create = createShortUrlUseCase.create(
                    url = trimmedUri,
                    data = ShortUrlProperties(),
                    qrRequest = qrRequest
                )
            }
            catch (e : Exception) {
                error = e.message.toString()
            }
            if (error == "no_error") {
                // Obtener el enlace
                val shortUrl = linkToService.link(create.hash).toString()
                logger.info("Enviando mensaje: $shortUrl")
                val address = session.localAddress
                val codedUri = "http:/$address$shortUrl"
                val qrUrl = if (qrRequest) "$codedUri/qr" else "no_qr"
                val final = "$trimmedUri,$codedUri,$qrUrl,$error"
                // Enviar mensaje a través de la sesión WebSocket
                synchronized(creationLock) {
                    session.sendMessage(TextMessage(final))
                }
            } else {
                val final = "$trimmedUri,no_url,no_qr,$error"
                synchronized(creationLock) {
                    // Enviar mensaje a través de la sesión WebSocket
                    session.sendMessage(TextMessage(final))
                }
            }
        }
    }

    private fun detectDelimiter(line: String): Char {
        val delimiters = listOf(',', ';', '\t', '|')
        var maxCount = 0
        var mostLikelyDelimiter = ','

        for (delimiter in delimiters) {
            val count = line.count { it == delimiter }
            if (count > maxCount) {
                maxCount = count
                mostLikelyDelimiter = delimiter
            }
        }

        return mostLikelyDelimiter
    }
}

