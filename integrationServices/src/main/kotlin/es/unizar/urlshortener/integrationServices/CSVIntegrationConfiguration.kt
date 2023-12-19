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
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * Configuración para la integración de CSV en la aplicación Spring.
 */
@Configuration
@EnableIntegration
@EnableScheduling
class CSVIntegrationConfiguration(
        private val linkToService: LinkToService
) {

    companion object {
        private const val CSV_CREATION_CORE_POOL_SIZE = 2
        private const val CSV_CREATION_MAX_POOL_SIZE = 5
        private const val CSV_CREATION_QUEUE_CAPACITY = 25
        private const val CSV_CREATION_THREAD_NAME = "csv-update-"
    }

    private val logger: Logger = LoggerFactory.getLogger(CSVIntegrationConfiguration::class.java)
    private val creationLock = Object()

    /**
     * Devuelve un [Executor] para la ejecución de operaciones relacionadas con la creación de CSV.
     *
     * @return El [Executor] para la creación de CSV.
     */
    fun csvCreationExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = CSV_CREATION_CORE_POOL_SIZE
        maxPoolSize = CSV_CREATION_MAX_POOL_SIZE
        setQueueCapacity(CSV_CREATION_QUEUE_CAPACITY)
        setThreadNamePrefix(CSV_CREATION_THREAD_NAME)
        initialize()
    }

    /**
     * Devuelve un canal [MessageChannel] asociado al [Executor] para la creación de CSV.
     *
     * @return El canal [MessageChannel] para la creación de CSV.
     */
    @Bean
    fun csvCreationChannel(): MessageChannel = ExecutorChannel(csvCreationExecutor())

    /**
     * Define un flujo de integración para la creación de CSV.
     *
     * @param createShortUrlUseCase La instancia de [CreateShortUrlUseCaseImpl].
     * @return El flujo de integración para la creación de CSV.
     */
    @Bean
    @Suppress("TooGenericExceptionCaught")
    fun csvFlow(createShortUrlUseCase: CreateShortUrlUseCaseImpl): IntegrationFlow =
            integrationFlow(csvCreationChannel()) {
        filter<Pair<String, WebSocketSession>> { payload ->
            val (uri, _) = payload
            val delimiter = detectDelimiter(uri)
            !uri.startsWith("URI${delimiter}QR")
        }
        handle<Pair<String, WebSocketSession>> { payload, _ ->
            val (uri, session) = payload
            logger.info("Procesando URI: $uri")
            val delimiter = detectDelimiter(uri)
            val parts = uri.split(delimiter)
            val trimmedUri = parts[0].trim()
            val qrRequest = parts[1].trim() == "1"
            var error = "no_error"
            lateinit var create: es.unizar.urlshortener.core.ShortUrl
            logger.info("Procesando URI: $trimmedUri")
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

            val safe: String = if (create.properties.safe == null) {
                "URI de destino no validada todavia"
            } else {
                if (create.properties.safe==true) {
                    "URL segura"
                } else {
                    "URL no segura"
                }
            }

            if (error == "no_error") {
                val shortUrl = linkToService.link(create.hash).toString()
                logger.info("Enviando mensaje: $shortUrl")
                val address = session.localAddress
                val codedUri = "http:/$address$shortUrl"
                val qrUrl = if (qrRequest) "$codedUri/qr" else "no_qr"
                val final = "$trimmedUri,$codedUri,$qrUrl,$error,$safe"
                // Enviar mensaje a través de la sesión WebSocket
                synchronized(creationLock) {
                    logger.info("Enviando mensaje: $final")
                    session.sendMessage(TextMessage(final))
                }
            } else {
                val final = "$trimmedUri,no_url,no_qr,$error,$safe"
                synchronized(creationLock) {
                    logger.info("Enviando mensaje: $final")
                    session.sendMessage(TextMessage(final))
                }
            }
        }
    }

    /**
     * Detecta el delimitador más probable en una línea dada.
     *
     * @param line La línea en la que se buscará el delimitador.
     * @return El delimitador más probable en la línea.
     */
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
        logger.info("Delimiter detected: $mostLikelyDelimiter")
        return mostLikelyDelimiter
    }
}

/**
 * Configuración para WebSocket en la aplicación Spring.
 */
@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {

    /**
     * Registra el manejador WebSocket para la ruta "/api/fast-bulk".
     *
     * @param registry El registro de manejadores WebSocket.
     */
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(myHandler(), "/api/fast-bulk")
                .addInterceptors(HttpSessionHandshakeInterceptor())
                .setAllowedOrigins("*")
    }

    /**
     * Devuelve una instancia de [MyWebSocketHandler].
     *
     * @return La instancia de [MyWebSocketHandler].
     */
    fun myHandler(): MyWebSocketHandler {
        return MyWebSocketHandler()
    }
}

/**
 * Handler de  WebSocket para manejar mensajes de texto.
 */
class MyWebSocketHandler : TextWebSocketHandler() {

    private val logger: Logger = LoggerFactory.getLogger(MyWebSocketHandler::class.java)

    /**
     * Maneja el mensaje de texto recibido.
     *
     * @param session La sesión WebSocket.
     * @param message El mensaje de texto recibido.
     */
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        logger.info("Received message: ${message.payload}")
        val sendCsvBean = SpringContext.getBean(CSVRequestGateway::class.java)
        sendCsvBean.sendCSVMessage(Pair(message.payload, session))
    }

    /**
     * Se llama después de que se establece la conexión WebSocket.
     *
     * @param session La sesión WebSocket.
     */
    override fun afterConnectionEstablished(session: WebSocketSession) {
        val localAddress = session.attributes["localAddress"]
        session.sendMessage(TextMessage("Hello from server"))
        logger.info("WebSocket session established from local address: $localAddress")
    }

}
