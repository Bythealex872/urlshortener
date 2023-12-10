package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream

interface CreateQRCodeUseCase {
    fun createQRCode(id: String): ByteArray
    fun getQRCode(id: String): ByteArray
}

/**
 * Implementation of [CreateQRCodeUseCase].
 */
class CreateQRCodeUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
) : CreateQRCodeUseCase {
    
    override fun createQRCode(id: String): ByteArray { 
        val qrCodeImage = generateQrCodeImage(id)
        return convertToByteArray(qrCodeImage)
    }

    override fun getQRCode(id: String): ByteArray {
        val shortUrl = shortUrlRepository.findByKey(id)

        // Verifica si la URI recortada no existe
        if (shortUrl == null) {
            throw RedirectionNotFound(id)
        }
        if(!shortUrl.properties.safe || shortUrl.properties.qr == null){
            throw RetryAfterException()
        }
        if(shortUrl.redirection.mode != 307){
            throw RedirectionForbiden(id)
        }

        return shortUrl.properties.qr
    }

    companion object {
        const val QR_CODE_WIDTH = 400
        const val QR_CODE_HEIGHT = 400
    }

    private fun generateQrCodeImage(url: String): BufferedImage {
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
    
        return image
    }

    private fun convertToByteArray(image: BufferedImage): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", outputStream)
        return outputStream.toByteArray()
    }
}

