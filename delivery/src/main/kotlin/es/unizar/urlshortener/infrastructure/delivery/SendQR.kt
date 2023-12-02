package es.unizar.urlshortener.infrastructure.delivery

import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.Gateway

@MessagingGateway
interface SendQR {

    @Gateway(requestChannel = "qrCreationChannel")
    fun sendQR(url: String)
}
