package es.unizar.urlshortener.infrastructure.delivery

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

@MessagingGateway
interface SendQR {

    @Gateway(requestChannel = "qrCreationChannel")
    fun sendQR(url: String)
}
