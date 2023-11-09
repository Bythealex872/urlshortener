package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.image.BufferedImage

interface CreateQRCodeUseCase {
    fun createQRCode(key: String): BufferedImage
}

/**
 * Implementation of [CreateQRCodeUseCase].
 */
class CreateQRCodeUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : CreateQRCodeUseCase {
    override fun createQRCode(key: String): BufferedImage { 
        val redirection = shortUrlRepository.findByKey(key)?.redirection
            ?: throw RedirectionNotFound(key)

        return generateQrCodeImage(redirection.target.toString())
    }

    companion object {
        const val QR_CODE_WIDTH = 500
        const val QR_CODE_HEIGHT = 500
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
}

