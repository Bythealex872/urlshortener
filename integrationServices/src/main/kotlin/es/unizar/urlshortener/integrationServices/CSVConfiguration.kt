package es.unizar.urlshortener.integrationServices

import es.unizar.urlshortener.core.LinkToService
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import org.springframework.web.socket.server.standard.ServerEndpointExporter
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
import org.springframework.web.socket.config.annotation.EnableWebSocket
import java.util.concurrent.Executor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
//import jakarta.websocket.*
import jakarta.websocket.Session

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
class WebSocketConfig{
    @Bean
    fun serverEndpointExporter() = ServerEndpointExporter()
}


@Configuration
@EnableIntegration
@EnableScheduling
class CSVCodeIntegrationConfiguration(
    private val linkToService: LinkToService
) {

    companion object {
        private const val CSV_CREATION_CORE_POOL_SIZE = 2
        private const val CSV_CREATION_MAX_POOL_SIZE = 5
        private const val CSV_CREATION_QUEUE_CAPACITY = 25
        private const val CSV_CREATION_THREAD_NAME = "csv-update-"
    }

    private val logger: Logger = LoggerFactory.getLogger(CSVCodeIntegrationConfiguration::class.java)

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
    fun csvFlow(createShortUrlUseCase: CreateShortUrlUseCase): IntegrationFlow =
            integrationFlow(csvCreationChannel()) {
        transform<Pair<String,Session >>{ payload ->

            logger.info("Debug")
            val parts = payload.first.split(",")
            val uri = parts[0].trim()
            val qr = parts[1].trim()
            val create = createShortUrlUseCase.create(
                url = uri,
                data = ShortUrlProperties()
            )
            val aa = linkToService.link(create.hash)
            payload.second.basicRemote.sendText("hola")
            true
        }

    }
}






/* 
@Component
class SpringContext : ApplicationContextAware {

    override fun setApplicationContext(context: ApplicationContext) {
        SpringContext.context = context
    }

    fun getApplicationContext(): ApplicationContext {
        return context
    }

    companion object {
        private lateinit var context: ApplicationContext
        
        internal fun setContext(applicationContext: ApplicationContext) {
            context = applicationContext
        }

        inline fun <reified T> getBean(): T {
            return context.getBean(T::class.java)
        }
    }
}*/
