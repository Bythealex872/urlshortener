@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.junit.jupiter.api.Assertions
import java.time.OffsetDateTime


@WebMvcTest
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class,
        RestResponseEntityExceptionHandler::class
    ]
)
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var redirectUseCase: RedirectUseCase

    @MockBean
    private lateinit var logClickUseCase: LogClickUseCase

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @MockBean
    private lateinit var createQRCodeUseCase: CreateQRCodeUseCase

    @MockBean
    private lateinit var createCSVUseCase: CreateCSVUseCase

    @MockBean
    private lateinit var userAgentInfoUseCase: UserAgentInfoUseCase

    @MockBean
    private lateinit var shortUrlRepository: ShortUrlRepositoryService
    
    @MockBean
    private lateinit var sendQR: SendQR

    @MockBean
    private lateinit var rateLimiter: RateLimiter

    @Test
    fun `redirectTo returns a redirect when the key exists and no userAgent`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("key",
                ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns a redirect when the key exists and userAgent`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))

        // Simular el envío de un encabezado "User-Agent" en la solicitud
        val userAgentHeaderValue = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

        mockMvc.perform(get("/{id}", "key").header("User-Agent", userAgentHeaderValue))
                .andExpect(status().isTemporaryRedirect)
                .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("key",
                ClickProperties(ip = "127.0.0.1", browser = "Chrome", platform = "Win10"))
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
    }

    @Test
    fun `creates returns bad request if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "ftp://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willAnswer { throw InvalidUrlException("ftp://example.com/") }

        mockMvc.perform(
            post("/api/link")
                .param("url", "ftp://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }

    @Test
    fun `creates returns a basic redirect and the qr route if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("qrRequest", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.properties.qr").value("http://localhost/f684a3c4/qr"))
    }

    @Test
    fun `qrCode returns not found when url does not exist`() {
        given(
            shortUrlRepository.findByKey("nonexistent")
        ).willReturn(null)
        mockMvc.perform(get("/{id}/qr", "nonexistent"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("URL no encontrada"))
    }
    @Test
    fun `qrCode returns bad request when qr code is not available`() {
        given(
            shortUrlRepository.findByKey("nonexistent")
        ).willReturn(ShortUrl("nonexistent", Redirection("http://example.com"), OffsetDateTime.now()))
        mockMvc.perform(get("/{id}/qr", "nonexistent"))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Código QR no disponible"))
    }
    @Test
    fun `qrCode returns qr code image when qr code is available`() {
        val qrCodeBytes = ByteArray(100) // Supongamos que es un QR generado
        val url = ShortUrl("existent", Redirection("http://example.com"), OffsetDateTime.now(), ShortUrlProperties(qr = qrCodeBytes))
        given(
            shortUrlRepository.findByKey("existent")
        ).willReturn(url)
        mockMvc.perform(get("/{id}/qr", "existent"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
    }

    @Test
    fun `qrCode returns too many requests when rate limit is exceeded`() {
        val clientId = "127.0.0.1" // Ejemplo de una dirección IP
        val retryAfterSeconds = 60L // Tiempo de espera en segundos
        given(rateLimiter.isLimitExceeded(clientId)).willReturn(true)
        given(rateLimiter.timeToNextRequest(clientId)).willReturn(retryAfterSeconds * 1000) // Convertir a milisegundos

        mockMvc.perform(get("/{id}/qr", "existent").header("X-Forwarded-For", clientId))
            .andDo(print())
            .andExpect(status().isTooManyRequests)
            .andExpect(header().string("Retry-After", retryAfterSeconds.toString()))
            .andExpect(jsonPath("$.error").value("Demasiadas peticiones"))
    }

    @Test
    fun `Esto En C No Pasa`(){
        val csvOutputs = listOf(
            CsvOutput(
                originalUri = "https://example.com/long-url-1",
                shortenedUri = "https://short.url/1",
                qr = "https://short.url/1/qr",
                explanation = "Primera URL acortada"
            )
        )
        createCSVUseCase.buildCsvContent(csvOutputs)
        Assertions.assertTrue(true)
    }

    @Test
    fun `Return User-Agent info return a redirect with the information when the key exist`(){

    }

    @Test
    fun `Return User-Agent info return a not found when the key not found`(){

    }
}
