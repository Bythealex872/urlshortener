package es.unizar.urlshortener

import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import org.springframework.integration.channel.DirectChannel
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
import org.springframework.integration.dsl.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository
) {
    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)

    @Bean
    fun validatorService() = ValidatorServiceImpl()

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    @Bean
    fun createShortUrlUseCase() =
        CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), hashService())
        
    @Bean
    fun createQRCodeUseCase() = CreateQRCodeUseCaseImpl()
    
    @Bean
    fun createCSVUseCase() = CreateCSVUseCaseImpl()

    @Bean
    fun userAgentInfoUseCase() = UserAgentInfoUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun aaa() = QRCodeIntegrationConfig(shortUrlRepositoryService())

}

@Configuration
@EnableIntegration
@EnableScheduling
class QRCodeIntegrationConfig(
    private val shortUrlRepository: ShortUrlRepositoryService,
) {

    private val logger: Logger = LoggerFactory.getLogger(QRCodeIntegrationConfig::class.java)


    @Bean
    fun inputChannel(): MessageChannel = DirectChannel()

    @Bean
    fun outputChannel(): MessageChannel = PublishSubscribeChannel()

    @Bean
    fun qrCodeFlow(createQRCodeUseCase: CreateQRCodeUseCase): IntegrationFlow = integrationFlow {
        channel(inputChannel())
        transform { url: String -> 
            val qrCode = createQRCodeUseCase.createQRCode(url)
            QrCodeUpdatePayload(url, qrCode)
        }
        handle<QrCodeUpdatePayload> { payload, _ ->
            shortUrlRepository.updateQRCodeByHash(payload.url, "holaaaaaaaa")
            logger.info("QR code updated for URL: ${payload.url}")
        }
    }

    data class QrCodeUpdatePayload(val url: String, val qrCode: ByteArray)
}
