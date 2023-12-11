package es.unizar.urlshortener.infrastructure.delivery

import org.springframework.integration.annotation.Gateway
import org.springframework.integration.annotation.MessagingGateway

@MessagingGateway
interface SendSafeBrowser {

    @Gateway(requestChannel = "safeBrowsingChannel")
    fun sendSafeBrowser(p: Pair<String, String>)
}
