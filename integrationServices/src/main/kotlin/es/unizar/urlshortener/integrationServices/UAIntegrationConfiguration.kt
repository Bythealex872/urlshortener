package es.unizar.urlshortener.integrationServices

import es.unizar.urlshortener.core.ClickRepositoryService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.ExecutorChannel
import org.springframework.messaging.MessageChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.scheduling.annotation.EnableScheduling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.UserAgent
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.UserAgentInfoUseCase
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.integrationFlow
import java.util.concurrent.Executor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableIntegration
@EnableScheduling
class UAIntegrationConfiguration {

    companion object {
        private const val UA_UPDATE_CORE_POOL_SIZE = 2
        private const val UA_UPDATE_MAX_POOL_SIZE = 5
        private const val UA_UPDATE_QUEUE_CAPACITY = 25
        private const val UA_UPDATE_THREAD_NAME = "ua-update-"
    }

    private val logger: Logger = LoggerFactory.getLogger(UAIntegrationConfiguration::class.java)

    fun uaUpdateExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = UA_UPDATE_CORE_POOL_SIZE
        maxPoolSize = UA_UPDATE_MAX_POOL_SIZE
        setQueueCapacity(UA_UPDATE_QUEUE_CAPACITY)
        setThreadNamePrefix(UA_UPDATE_THREAD_NAME)
        initialize()
    }
    @Bean
    fun uaUpdateChannel(): MessageChannel = ExecutorChannel(uaUpdateExecutor())


    @Bean
    fun uaFlow(logClickUseCase: LogClickUseCase): IntegrationFlow = integrationFlow(uaUpdateChannel()) {
        handle<Triple<String, String, String?>> { payload, _ ->
            logClickUseCase.logClick(payload.first, payload.second, payload.third)
            logger.info("Código UA creado para ${payload.first}")
        }
    }

}
