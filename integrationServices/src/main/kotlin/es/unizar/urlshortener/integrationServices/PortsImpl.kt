package es.unizar.urlshortener.integrationServices

import es.unizar.urlshortener.core.QRRequestService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.Gateway
import org.springframework.stereotype.Component
@Component
class QRRequestImpl : QRRequestService {
    @Autowired
    lateinit var qrRequestGateway: QRRequestGateway
    override fun requestQRcreation(p: Pair<String, String>) {
        qrRequestGateway.sendToQRChannel(p)
    }
}
@MessagingGateway
interface QRRequestGateway {
    @Gateway(requestChannel = "qrCreationChannel")
    fun sendToQRChannel(p: Pair<String, String>)
}
