package es.unizar.urlshortener.integrationServices

import es.unizar.urlshortener.core.QRRequestService
import es.unizar.urlshortener.core.SafeBrowsingRequestService
import es.unizar.urlshortener.core.UserAgentRequestService
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.Gateway
import org.springframework.web.socket.WebSocketSession

/**
 * Interfaz de mensajería para la solicitud de generación de códigos QR.
 */
@MessagingGateway
interface QRRequestGateway : QRRequestService {

    /**
     * Envia un mensaje para la generación de un código QR.
     *
     * @param p Par que contiene la URL original y la clave asociada al código QR.
     */
    @Gateway(requestChannel = "qrCreationChannel")
    override fun sendQRMessage(p: Pair<String, String>)
}

/**
 * Interfaz de mensajería para la solicitud de información del agente de usuario.
 */
@MessagingGateway
interface UserAgentRequestGateway : UserAgentRequestService {

    /**
     * Envia un mensaje para obtener información del agente de usuario.
     *
     * @param p Triple que contiene la clave, la dirección IP y la cadena del agente de usuario.
     */
    @Gateway(requestChannel = "uaUpdateChannel")
    override fun sendUserAgentMessage(p: Triple<String, String,String?>)
}

/**
 * Interfaz de mensajería para la solicitud de verificación de seguridad de una URL.
 */
@MessagingGateway
interface SafeBrowsingRequestGateway : SafeBrowsingRequestService {

    /**
     * Envia un mensaje para verificar la seguridad de una URL.
     *
     * @param p Par que contiene la URL y la clave asociada.
     */
    @Gateway(requestChannel = "safeBrowsingChannel")
    override fun sendSafeBrowsingMessage(p: Pair<String, String>)
}

/**
 * Interfaz de mensajería para la solicitud de procesamiento de URI desde un archivo CSV.
 */
@MessagingGateway
interface CSVRequestGateway {

    /**
     * Envia un mensaje para procesar una URI desde un archivo CSV.
     *
     * @param p Par que contiene la URI y la sesión WebSocket asociada.
     */
    @Gateway(requestChannel = "csvCreationChannel")
    fun sendCSVMessage(p: Pair<String, WebSocketSession>)
}
