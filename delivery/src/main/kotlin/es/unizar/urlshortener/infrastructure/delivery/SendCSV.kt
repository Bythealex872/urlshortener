package es.unizar.urlshortener.infrastructure.delivery

import jakarta.websocket.Session
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.Gateway

@MessagingGateway
interface SendCSV {

    @Gateway(requestChannel = "csvCreationChannel")
    fun sendCSV(p: Pair<String, Session>)
}
