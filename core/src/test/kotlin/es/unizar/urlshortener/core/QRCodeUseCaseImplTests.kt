@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core

import es.unizar.urlshortener.core.usecases.QRCodeUseCaseImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class QRCodeUseCaseImplTests {

    private lateinit var shortUrlRepository: ShortUrlRepositoryService
    private lateinit var qrCodeUseCase: QRCodeUseCaseImpl

    @BeforeEach
    fun setUp() {
        shortUrlRepository = mock()
        qrCodeUseCase = QRCodeUseCaseImpl(shortUrlRepository)
    }

    @Test
    fun `createQRCode should generate QR code`() {
        val id = "testId"
        val qrCode = qrCodeUseCase.createQRCode(id)

        assertNotNull(qrCode)
        assertTrue(qrCode.isNotEmpty())
    }

    @Test
    fun `getQRCode should throw RedirectionNotFound when URL is not found`() {
        val id = "unknownId"
        whenever(shortUrlRepository.findByKey(id)).thenReturn(null)

        assertThrows<RedirectionNotFound> {
            qrCodeUseCase.getQRCode(id)
        }
    }

    @Test
    fun `getQRCode should throw RetryAfterException when URL is not validated yet`() {
        val id = "unvalidatedId"
        val shortUrl = ShortUrl(id, Redirection("http://example.com"), properties = ShortUrlProperties(safe = null))

        whenever(shortUrlRepository.findByKey(id)).thenReturn(shortUrl)

        assertThrows<RetryAfterException> {
            qrCodeUseCase.getQRCode(id)
        }
    }

    @Test
    fun `getQRCode should throw RedirectionForbidden when URL is not safe`() {
        val id = "unsafeId"
        val shortUrl = ShortUrl(id, Redirection("http://example.com"), properties = ShortUrlProperties(safe = false))

        whenever(shortUrlRepository.findByKey(id)).thenReturn(shortUrl)

        assertThrows<RedirectionForbidden> {
            qrCodeUseCase.getQRCode(id)
        }
    }

    @Test
    fun `getQRCode should return QR code when URL is safe and QR exists`() {
        val id = "safeId"
        val qrCode = generateFakeQRCode()
        val shortUrl = ShortUrl(id, Redirection("http://example.com"),
                properties = ShortUrlProperties(safe = true, qr = qrCode))

        whenever(shortUrlRepository.findByKey(id)).thenReturn(shortUrl)

        val result = qrCodeUseCase.getQRCode(id)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertArrayEquals(qrCode, result)
    }

    private fun generateFakeQRCode(): ByteArray {
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", outputStream)
        return outputStream.toByteArray()
    }
}
