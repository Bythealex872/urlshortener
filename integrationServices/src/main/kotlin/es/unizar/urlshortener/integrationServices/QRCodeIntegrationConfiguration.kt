package es.unizar.urlshortener.integrationServices

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.ExecutorChannel
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.integrationFlow
import org.springframework.messaging.MessageChannel
import org.springframework.integration.annotation.ServiceActivator
import es.unizar.urlshortener.core.usecases.QRCodeUseCase
import org.springframework.integration.config.EnableIntegration
import org.springframework.scheduling.annotation.EnableScheduling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import java.util.concurrent.Executor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableIntegration
@EnableScheduling
class QRCodeIntegrationConfiguration(
    private val shortUrlRepository: ShortUrlRepositoryService
) {

    companion object {
        private const val QR_CREATION_CORE_POOL_SIZE = 2
        private const val QR_CREATION_MAX_POOL_SIZE = 5
        private const val QR_CREATION_QUEUE_CAPACITY = 25
        private const val QR_CREATION_THREAD_NAME = "qr-creation-"

        private const val QR_UPDATE_CORE_POOL_SIZE = 2
        private const val QR_UPDATE_MAX_POOL_SIZE = 5
        private const val QR_UPDATE_QUEUE_CAPACITY = 25
        private const val QR_UPDATE_THREAD_NAME = "qr-update-"
    }

    private val logger: Logger = LoggerFactory.getLogger(QRCodeIntegrationConfiguration::class.java)

    data class QRCodePayload(val id: String, val qrCode: ByteArray)

    fun qrCreationExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = QR_CREATION_CORE_POOL_SIZE
        maxPoolSize = QR_CREATION_MAX_POOL_SIZE
        setQueueCapacity(QR_CREATION_QUEUE_CAPACITY)
        setThreadNamePrefix(QR_CREATION_THREAD_NAME)
        initialize()
    }

    fun qrUpdateExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = QR_UPDATE_CORE_POOL_SIZE
        maxPoolSize = QR_UPDATE_MAX_POOL_SIZE
        setQueueCapacity(QR_UPDATE_QUEUE_CAPACITY)
        setThreadNamePrefix(QR_UPDATE_THREAD_NAME)
        initialize()
    }

    @Bean
    fun qrCreationChannel(): MessageChannel = ExecutorChannel(qrCreationExecutor())

    @Bean
    fun qrUpdateChannel(): MessageChannel = ExecutorChannel(qrUpdateExecutor())

    @Bean
    fun qrFlow(createQRCodeUseCase: QRCodeUseCase): IntegrationFlow = integrationFlow(qrCreationChannel()) {
        filter<Pair<String, String>> { payload ->
            shortUrlRepository.findByKey(payload.first)?.properties?.qr == null
        }
        transform<Pair<String, String>>  { payload -> 
            val qrCode = createQRCodeUseCase.createQRCode(payload.second)
            logger.info("Código QR creado para ${payload.first}")
            QRCodePayload(payload.first, qrCode)
        } 
        channel(qrUpdateChannel())
    }

    @ServiceActivator(inputChannel = "qrUpdateChannel")
    fun updateDatabase(payload: QRCodePayload) {
        shortUrlRepository.updateQRCodeByHash(payload.id, payload.qrCode)
        logger.info("Código QR actualizado para ${payload.id}")
    }
}
