package es.unizar.urlshortener.infrastructure.delivery

import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.Gateway

@MessagingGateway
interface SendUA{

    @Gateway(requestChannel = "qrCreationChannel")
    fun sendUA(p: Pair<String, String>)
}
