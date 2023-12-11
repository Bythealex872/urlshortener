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
import es.unizar.urlshortener.core.usecases.UserAgentInfoUseCase
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.integrationFlow
import java.util.concurrent.Executor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableIntegration
@EnableScheduling
class UAIntegrationConfiguration(
        private val shortUrlRepository: ShortUrlRepositoryService,
        private val clickRepository: ClickRepositoryService
) {

    companion object {
        private const val UA_RETURN_CORE_POOL_SIZE = 2
        private const val UA_RETURN_MAX_POOL_SIZE = 5
        private const val UA_RETURN_QUEUE_CAPACITY = 25
        private const val UA_RETURN_THREAD_NAME = "ua-creation-"

        private const val UA_UPDATE_CORE_POOL_SIZE = 2
        private const val UA_UPDATE_MAX_POOL_SIZE = 5
        private const val UA_UPDATE_QUEUE_CAPACITY = 25
        private const val UA_UPDATE_THREAD_NAME = "ua-update-"
    }

    private val logger: Logger = LoggerFactory.getLogger(UAIntegrationConfiguration::class.java)

    data class UAPayload(val id: String, val ua: UserAgent)

    fun uaReturnExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = UA_RETURN_CORE_POOL_SIZE
        maxPoolSize = UA_RETURN_MAX_POOL_SIZE
        setQueueCapacity(UA_RETURN_QUEUE_CAPACITY)
        setThreadNamePrefix(UA_RETURN_THREAD_NAME)
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
    fun uaReturnChannel(): MessageChannel = ExecutorChannel(uaReturnExecutor())

    @Bean
    fun uaUpdateChannel(): MessageChannel = ExecutorChannel(uaUpdateExecutor())


    @Bean
    fun uaFlow(userAgentInfo: UserAgentInfoUseCase): IntegrationFlow = integrationFlow(uaReturnChannel()) {
        filter<Pair<String, String>> { payload ->
            shortUrlRepository.findByKey(payload.first) != null
        }
        transform<Pair<String, String>>  { payload ->
            val ua = userAgentInfo.returnUserAgentInfo(payload.second)
            logger.info("Código UA creado para ${payload.first}")
            if (ua != null) {
                UAPayload(payload.first, ua)
            }
        }
        channel(uaUpdateChannel())
    }
    @ServiceActivator(inputChannel = "uaUpdateChannel")
    fun updateDatabase(payload: UAPayload) {
        payload.ua.browser?.let {
            payload.ua.platform?.let { it1 -> clickRepository.updateUAByIp(payload.id, it, it1) } }
        logger.info("UA actualizado para ${payload.id}")
    }
}
