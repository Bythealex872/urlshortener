package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.RedirectionNotFound
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import es.unizar.urlshortener.core.RedirectionForbidden
import es.unizar.urlshortener.core.RetryAfterException
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream

interface QRCodeUseCase {
    /** Método para generar un código QR a partir de una ID
    * @param id: Identificador único asociado a una URL corta
    * @return ByteArray: Representación de bytes del código QR generado
    */
    fun createQRCode(id: String): ByteArray
    /** Método para obtener el código QR asociado a una ID
    * @param id: Identificador único asociado a una URL corta
    * @return ByteArray: Representación de bytes del código QR correspondiente a la ID
    */

    fun getQRCode(id: String): ByteArray
}

/**
 * Implementation of [QRCodeUseCase].
 */
class QRCodeUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
) : QRCodeUseCase {
    private val logger: Logger = LoggerFactory.getLogger(QRCodeUseCaseImpl::class.java)
    /** Implementación del método para generar un código QR
    * @param id: Identificador único asociado a una URL corta
    * @return ByteArray: Representación de bytes del código QR generado
    */
    override fun createQRCode(id: String): ByteArray {
        logger.info("Generando QR para $id")
        val qrCodeImage = generateQrCodeImage(id)
        return convertToByteArray(qrCodeImage)
    }
    /** Implementación del método para obtener el código QR
    * @param id: Identificador único asociado a una URL corta
    * @return ByteArray: Representación de bytes del código QR correspondiente a la ID
    */
    override fun getQRCode(id: String): ByteArray {
        logger.info("Buscando $id para crear qr")
        val shortUrl = shortUrlRepository.findByKey(id)
        if(shortUrl == null){
            logger.error("No se ha encontrado la URI recortada")
            throw RedirectionNotFound(id)
        }
        if(shortUrl.properties.safe == null){
            logger.error("No se ha validado la URL todavia")
            throw RetryAfterException()
        }
        if(shortUrl.properties.qr == null){
            logger.error("No se ha generado el QR todavia")
            throw RetryAfterException()
        }
        if(!shortUrl.properties.safe){
            logger.error("La URI recortada no es segura")
            throw RedirectionForbidden(id)
        }

        logger.info("Devolviendo el QR de $id")
        return shortUrl.properties.qr
    }

    companion object {
        const val QR_CODE_WIDTH = 400
        const val QR_CODE_HEIGHT = 400
    }
    /** Método para generar una imagen de código QR a partir de una URL
    * -@param url: URL para la cual se generará el código QR
    * @return BufferedImage: Imagen que representa el código QR generado
    */
    private fun generateQrCodeImage(url: String): BufferedImage {
        logger.info("Generando QR para $url")
        val qrCodeWriter = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
        )
        val bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT, hints)
    
        val width = bitMatrix.width
        val height = bitMatrix.height
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    
        for (x in 0 until width) {
            for (y in 0 until height) {
                image.setRGB(x, y, if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }

        logger.info("QR generado para $url")
        return image
    }
    /** Método para convertir una imagen a un array de bytes
    * @param image: Imagen a convertir
    * @return ByteArray: Representación de bytes de la imagen convertida
    */
    private fun convertToByteArray(image: BufferedImage): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", outputStream)
        return outputStream.toByteArray()
    }
}

