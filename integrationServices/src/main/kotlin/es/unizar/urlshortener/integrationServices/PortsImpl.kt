package es.unizar.urlshortener.integrationServices

import es.unizar.urlshortener.core.QRRequestService
import es.unizar.urlshortener.core.SafeBrowsingRequestService
import es.unizar.urlshortener.core.UserAgentRequestService
import jakarta.websocket.Session
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.Gateway

@MessagingGateway
interface QRRequestGateway : QRRequestService {
    @Gateway(requestChannel = "qrCreationChannel")
    override fun sendQRMessage(p: Pair<String, String>)
}

@MessagingGateway
interface UserAgentRequestGateway : UserAgentRequestService {
    @Gateway(requestChannel = "uaReturnChannel")
    override fun sendUserAgentMessage(p: Triple<String, String,String?>)
}

@MessagingGateway
interface CSVRequestGateway {
    @Gateway(requestChannel = "csvCreationChannel")
    fun sendCSVMessage(p: Pair<String, Session>)
}

@MessagingGateway
interface SafeBrowsingRequestGateway : SafeBrowsingRequestService {
    @Gateway(requestChannel = "safeBrowsingChannel")
    override fun sendSafeBrowsingMessage(p: Pair<String, String>)
}
