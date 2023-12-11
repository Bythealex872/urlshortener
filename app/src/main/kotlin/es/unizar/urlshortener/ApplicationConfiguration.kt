@file:Suppress("WildcardImport", "NoWildcardImports", "TooManyFunctions")

package es.unizar.urlshortener

import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import es.unizar.urlshortener.integrationServices.QRCodeIntegrationConfiguration
import es.unizar.urlshortener.integrationServices.QRRequestImpl
import es.unizar.urlshortener.integrationServices.SafeBrowsingServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
    fun qrRequestService() = QRRequestImpl()

    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)
    @Bean
    fun safeBrowsingService() = SafeBrowsingServiceImpl()
    @Bean
    fun validatorService() = ValidatorServiceImpl()

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService(), logClickUseCase())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService(), userAgentInfoUseCase())

    @Bean
    fun createShortUrlUseCase() =
        CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), hashService())
    @Bean
    fun safeBrowsingUseCase() = SafeBrowsingUseCaseImpl(safeBrowsingService())
    @Bean
    fun createQRCodeUseCase() = QRCodeUseCaseImpl(shortUrlRepositoryService())
    
    @Bean
    fun createCSVUseCase() = CreateCSVUseCaseImpl()

    @Bean
    fun userAgentInfoUseCase() = UserAgentInfoUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun qrCodeIntegration() = QRCodeIntegrationConfiguration(shortUrlRepositoryService())

    @Bean
    fun safeBrowsingIntegration() = SafeBrowsingConfiguration(shortUrlRepositoryService())

}
