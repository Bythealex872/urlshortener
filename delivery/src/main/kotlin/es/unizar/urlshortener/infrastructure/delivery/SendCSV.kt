package es.unizar.urlshortener.infrastructure.delivery

import jakarta.websocket.Session
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.Gateway
import org.springframework.web.socket.WebSocketSession

@MessagingGateway
interface SendCSV {

    @Gateway(requestChannel = "csvCreationChannel")
    fun sendCSV(p: Pair<String, WebSocketSession>)
}
