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
@EnableIntegration
@EnableScheduling
class QRCodeIntegrationConfiguration(
    private val shortUrlRepository: ShortUrlRepositoryService
) {

    private val logger: Logger = LoggerFactory.getLogger(QRCodeIntegrationConfiguration::class.java)

    data class QRCodePayload(val url: String, val qrCode: ByteArray)

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
    fun qrFlow(createQRCodeUseCase: CreateQRCodeUseCase): IntegrationFlow = integrationFlow {
        channel(qrCreationChannel())
        transform { url: String -> 
            val qrCode = createQRCodeUseCase.createQRCode(url)
            logger.info("QR code created")
            QRCodePayload(url, qrCode)
        }
        channel(qrUpdateChannel())
    }

    @ServiceActivator(inputChannel = "qrUpdateChannel")
    fun updateDatabase(payload: QRCodePayload) {
        shortUrlRepository.updateQRCodeByHash(payload.url, payload.qrCode)
        logger.info("QR code updated for URL: ${payload.url}")
    }
}
