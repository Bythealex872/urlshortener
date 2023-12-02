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
import es.unizar.urlshortener.core.usecases.CreateQRCodeUseCase
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

@Configuration
@EnableWebSocket
class WebSocketConfig{
    @Bean
    fun serverEndpointExporter() = ServerEndpointExporter()
}

@EnableIntegration
@EnableScheduling
class CSVIntegrationConfiguration(
    private val shortUrlRepository: ShortUrlRepositoryService
) {

    /*
    private val logger: Logger = LoggerFactory.getLogger(CSVIntegrationConfiguration::class.java)

    data class CSVProcessingPayload(val line: String)

    companion object {
        private const val CSV_THREAD_POOL_SIZE = 5
        private const val CSV_QUEUE_CAPACITY = 25
        private const val CSV_THREAD_NAME = "csv-processing-"
    }

    fun csvExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = CSV_THREAD_POOL_SIZE
        maxPoolSize = CSV_THREAD_POOL_SIZE
        setQueueCapacity(CSV_QUEUE_CAPACITY)
        setThreadNamePrefix(CSV_THREAD_NAME)
        initialize()
    }

    @Bean
    fun csvProcessingChannel(): MessageChannel = ExecutorChannel(csvExecutor())

    @Bean
    fun csvProcessingFlow(processCSVUseCase: ProcessCSVUseCase): IntegrationFlow = integrationFlow {
        channel(csvProcessingChannel())
        transform { line: String -> 
            processCSVUseCase.processCSVLine(line)
            logger.info("CSV line processed: $line")
            CSVProcessingPayload(line)
        }
        handle(CSVProcessingPayload::class.java) { payload, headers ->
            // Additional processing, if needed
            logger.info("Additional processing for CSV line: ${payload.line}")
        }
    }
    */
}