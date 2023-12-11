package es.unizar.urlshortener.infrastructure.delivery

import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.Gateway
import jakarta.websocket.*
import jakarta.websocket.CloseReason.CloseCodes
import jakarta.websocket.server.ServerEndpoint

@MessagingGateway
interface SendCSV {

    @Gateway(requestChannel = "csvCreationChannel")
    fun SendCSV(p: Pair<String, Session>)
}
