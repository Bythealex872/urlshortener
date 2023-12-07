package es.unizar.urlshortener

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
import es.unizar.urlshortener.core.usecases.LogClickUseCase
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
@EnableIntegration
@EnableScheduling
class UACodeIntegrationConfiguration(
        private val shortUrlRepository: ShortUrlRepositoryService
) {

    companion object {
        private const val UA_CREATION_CORE_POOL_SIZE = 2
        private const val UA_CREATION_MAX_POOL_SIZE = 5
        private const val UA_CREATION_QUEUE_CAPACITY = 25
        private const val UA_CREATION_THREAD_NAME = "ua-creation-"

        private const val UA_UPDATE_CORE_POOL_SIZE = 2
        private const val UA_UPDATE_MAX_POOL_SIZE = 5
        private const val UA_UPDATE_QUEUE_CAPACITY = 25
        private const val UA_UPDATE_THREAD_NAME = "ua-update-"
    }

    private val logger: Logger = LoggerFactory.getLogger(UACodeIntegrationConfiguration::class.java)

    data class UACodePayload(val id: String, val uaCode: ByteArray)

    fun uaCreationExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = UA_CREATION_CORE_POOL_SIZE
        maxPoolSize = UA_CREATION_MAX_POOL_SIZE
        setQueueCapacity(UA_CREATION_QUEUE_CAPACITY)
        setThreadNamePrefix(UA_CREATION_THREAD_NAME)
        initialize()
    }

    fun uaUpdateExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = UA_UPDATE_CORE_POOL_SIZE
        maxPoolSize = UA_UPDATE_MAX_POOL_SIZE
        setQueueCapacity(UA_UPDATE_QUEUE_CAPACITY)
        setThreadNamePrefix(UA_UPDATE_THREAD_NAME)
        initialize()
    }

    @Bean
    fun uaCreationChannel(): MessageChannel = ExecutorChannel(uaCreationExecutor())

    @Bean
    fun uaUpdateChannel(): MessageChannel = ExecutorChannel(uaUpdateExecutor())

    /*
    @Bean
    fun uaFlow(createUACodeUseCase: CreateUACodeUseCase): IntegrationFlow = integrationFlow(uaCreationChannel()) {
        filter<Pair<String, String>> { payload ->
            shortUrlRepository.findByKey(payload.first)?.properties?.ua == null
        }
        transform<Pair<String, String>>  { payload ->
            val uaCode = createUACodeUseCase.createUACode(payload.second)
            logger.info("Código UA creado para ${payload.first}")
            UACodePayload(payload.first, uaCode)
        }
        channel(uaUpdateChannel())
    }

    @ServiceActivator(inputChannel = "uaUpdateChannel")
    fun updateDatabase(payload: UACodePayload) {
        shortUrlRepository.updateUACodeByHash(payload.id, payload.uaCode)
        logger.info("Código UA actualizado para ${payload.id}")
    }
    */
}
