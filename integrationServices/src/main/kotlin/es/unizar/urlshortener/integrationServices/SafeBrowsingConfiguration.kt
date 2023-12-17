package es.unizar.urlshortener.integrationServices

import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.usecases.SafeBrowsingUseCase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor
import org.springframework.integration.aggregator.MessageGroupProcessor
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.ExecutorChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.integrationFlow
import org.springframework.integration.store.MessageGroupStore
import org.springframework.integration.store.SimpleMessageStore
import org.springframework.messaging.MessageChannel
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

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
        private const val SAFE_CREATION_THREAD_NAME = "safe-browsing"

        private const val SAFE_UPDATE_CORE_POOL_SIZE = 2
        private const val SAFE_UPDATE_MAX_POOL_SIZE = 5
        private const val SAFE_UPDATE_QUEUE_CAPACITY = 25
        private const val SAFE_UPDATE_THREAD_NAME = "safe-update"
        private const val TIME_LIMIT = 1L
    }

    private val logger: Logger = LoggerFactory.getLogger(SafeBrowsingConfiguration::class.java)

    data class SafeBrowsingPayload(val url: String, val isSafe: Boolean)

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
    /*
    * Configuración del canal para Safe Browsing.
    */
    @Bean
    fun safeBrowsingChannel(): MessageChannel = ExecutorChannel(safeBrowsingExecutor())
    /*
    * Configuración del canal para la actualización del estado seguro.
    */
    @Bean
    fun safeUpdateChannel(): MessageChannel = ExecutorChannel(safeUpdateExecutor())
    /*
    * Configuración de la tienda de grupos de mensajes.
    */
    @Bean
    fun messageStore(): MessageGroupStore = SimpleMessageStore()

    /*
     * Configuración del procesador de grupo de mensajes como un Bean.
     */
    @Bean
    fun messageGroupProcessor(): MessageGroupProcessor = DefaultAggregatingMessageGroupProcessor()
    /*
    * Configuración del flujo de integración para Safe Browsing.
    */
    @Bean
    fun safeBrowsingFlow(safeBrowsingUseCase: SafeBrowsingUseCase): IntegrationFlow =
        integrationFlow(safeBrowsingChannel()) {
            filter<Pair<String, String>> { payload ->
                shortUrlRepository.findByKey(payload.first)?.properties?.safe == null
            }
            aggregate {
                // Establece el almacenamiento de mensajes y el procesador de grupo
                messageStore(messageStore())
                processor(messageGroupProcessor())

                // Configura el tamaño del lote (N URLs) y el tiempo límite (1 minuto)
                correlationStrategy { "defaultCorrelationId" }
                //releaseStrategy { group -> group.size() >= GROUP_SIZE }
                expireGroupsUponTimeout(true)
                sendPartialResultOnExpiry(true)
                groupTimeout(TimeUnit.MINUTES.toMillis(TIME_LIMIT))
            }
            // Envía el lote de URLs al servicio de Safe Browsing
            transform <List<Pair<String, String>>> { payload ->
                val urls =payload.map { it.second }
                val unsafeResults = safeBrowsingUseCase.urlsAreSafe(urls)
                urls.map { url ->
                    SafeBrowsingPayload(url, url !in unsafeResults)
                }
            }
            channel(safeUpdateChannel())
        }
    /*
    * Configuración del flujo de integración para la actualización del estado seguro.
     */
    @ServiceActivator(inputChannel = "safeUpdateChannel")
    fun updateDatabase(payloads: List<SafeBrowsingPayload>) {
        payloads.forEach { payload ->
            shortUrlRepository.updateSafeStatusByTarget(payload.url, payload.isSafe)
            if (payload.isSafe) {
                logger.info("Safe actualizado para ${payload.url} es segura")
            } else {
                logger.info("Safe actualizado para ${payload.url} NO es segura")
            }
        }
    }
}
