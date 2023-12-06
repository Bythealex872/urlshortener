package es.unizar.urlshortener

import org.springframework.web.socket.server.standard.ServerEndpointExporter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.ExecutorChannel
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.integrationFlow
import org.springframework.messaging.MessageChannel
import org.springframework.integration.channel.PublishSubscribeChannel
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.annotation.Gateway
import org.springframework.stereotype.Component
import org.springframework.scheduling.annotation.Scheduled
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import org.springframework.integration.config.EnableIntegration
import org.springframework.scheduling.annotation.EnableScheduling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import java.util.concurrent.Executor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
//import jakarta.websocket.*
import jakarta.websocket.CloseReason.CloseCodes
import jakarta.websocket.server.ServerEndpoint
import es.unizar.urlshortener.core.ShortUrlProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import jakarta.websocket.*



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
    private val shortUrlRepository: ShortUrlRepositoryService
) {

    companion object {
        private const val CSV_CREATION_CORE_POOL_SIZE = 2
        private const val CSV_CREATION_MAX_POOL_SIZE = 5
        private const val CSV_CREATION_QUEUE_CAPACITY = 25
        private const val CSV_CREATION_THREAD_NAME = "csv-update-"
    }

    private val logger: Logger = LoggerFactory.getLogger(CSVCodeIntegrationConfiguration::class.java)

    fun CSVCreationExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = CSV_CREATION_CORE_POOL_SIZE
        maxPoolSize = CSV_CREATION_MAX_POOL_SIZE
        setQueueCapacity(CSV_CREATION_QUEUE_CAPACITY)
        setThreadNamePrefix(CSV_CREATION_THREAD_NAME)
        initialize()
    }

    @Bean
    fun CSVCreationChannel(): MessageChannel = ExecutorChannel(CSVCreationExecutor())

    @Bean
    fun CSVFlow(createShortUrlUseCase: CreateShortUrlUseCase): IntegrationFlow = integrationFlow {
        channel(CSVCreationChannel())
        transform<Pair<String,Session >>  { payload -> 

            logger.info("Debug")
            val parts = payload.first.split(",")
            val uri = parts[0].trim()
            val qr = parts[1].trim()
            val create = createShortUrlUseCase.create(
                url = uri,
                data = ShortUrlProperties()
            )
            true
            payload.second.basicRemote.sendText("hola")   
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
