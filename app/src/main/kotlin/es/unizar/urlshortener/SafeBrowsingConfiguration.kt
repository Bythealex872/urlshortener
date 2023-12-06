package es.unizar.urlshortener

import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.usecases.SafeBrowsingUseCase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.ExecutorChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.integrationFlow
import org.springframework.messaging.MessageChannel
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableIntegration
@EnableScheduling
class SafeBrowsingConfiguration(
    private val shortUrlRepository: ShortUrlRepositoryService
) {
    companion object {
        private const val SAFE_CREATION_CORE_POOL_SIZE = 2
        private const val SAFE_CREATION_MAX_POOL_SIZE = 5
        private const val SAFE_CREATION_QUEUE_CAPACITY = 25
        private const val SAFE_CREATION_THREAD_NAME = "safe-browsinga"

        private const val SAFE_UPDATE_CORE_POOL_SIZE = 2
        private const val SAFE_UPDATE_MAX_POOL_SIZE = 5
        private const val SAFE_UPDATE_QUEUE_CAPACITY = 25
        private const val SAFE_UPDATE_THREAD_NAME = "safe-update"

    }
    private val logger: Logger = LoggerFactory.getLogger(SafeBrowsingConfiguration::class.java)
    data class SafeBrowsingPayload(val id: String, val isSafe: Boolean)
    fun safeBrowsingExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = SAFE_CREATION_CORE_POOL_SIZE
        maxPoolSize = SAFE_CREATION_MAX_POOL_SIZE
        setQueueCapacity(SAFE_CREATION_QUEUE_CAPACITY)
        setThreadNamePrefix(SAFE_CREATION_THREAD_NAME)
        initialize()
    }

    fun safeUpdateExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = SAFE_UPDATE_CORE_POOL_SIZE
        maxPoolSize = SAFE_UPDATE_MAX_POOL_SIZE
        setQueueCapacity(SAFE_UPDATE_QUEUE_CAPACITY)
        setThreadNamePrefix(SAFE_UPDATE_THREAD_NAME)
        initialize()
    }
    @Bean
    fun safeBrowsingChannel(): MessageChannel = ExecutorChannel(safeBrowsingExecutor())
    @Bean
    fun safeUpdateChannel(): MessageChannel = ExecutorChannel(safeUpdateExecutor())


    @Bean
    fun safeBrowsingFlow(safeBrowsingUseCase: SafeBrowsingUseCase): IntegrationFlow = integrationFlow {
        channel(safeBrowsingChannel())
        filter<Pair<String, String>> { payload ->
            shortUrlRepository.findByKey(payload.first)?.properties?.safe == null
        }
        transform<Pair<String, String>>  { payload ->
            val isSafe = safeBrowsingUseCase.urlisSafe(payload.second)
            logger.info("Consulta si la url ${payload.first} es segura")
            SafeBrowsingPayload(payload.first, isSafe)
        }
        channel(safeUpdateChannel())
    }

    @ServiceActivator(inputChannel = "safeUpdateChannel")
    fun updateDatabase(payload:SafeBrowsingPayload) {
        shortUrlRepository.updateSafeStatusByHash(payload.id, payload.isSafe)
        logger.info("Safe actualizado para ${payload.id}")
    }
}
