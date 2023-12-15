@file:Suppress("WildcardImport", "UnusedPrivateProperty")

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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.time.OffsetDateTime
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


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
    private lateinit var createQRCodeUseCase: QRCodeUseCase

    @MockBean
    private lateinit var createCSVUseCase: CreateCSVUseCase

    @MockBean
    private lateinit var userAgentInfoUseCase: UserAgentInfoUseCase

    //@MockBean
    //private lateinit var sendSafeBrowser: SendSafeBrowser

    @Test
    fun `redirectTo returns a redirect when the key exists and no userAgent`() {

        given(redirectUseCase.redirectTo("key", "127.0.0.1", "")).willReturn(Redirection("http://example.com/"))

        mockMvc.perform(get("/{id}", "key").header("User-Agent", ""))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        //verify(logClickUseCase).logClick("key", ip = "127.0.0.1", UA = "")
    }

    @Test
    fun `redirectTo returns a redirect when the key exists and userAgent`() {
        val userAgentHeaderValue = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

        given(redirectUseCase.redirectTo("key", "127.0.0.1", userAgentHeaderValue))
                .willReturn(Redirection("http://example.com/"))

        mockMvc.perform(get("/{id}", "key").header("User-Agent", userAgentHeaderValue))
                .andExpect(status().isTemporaryRedirect)
                .andExpect(redirectedUrl("http://example.com/"))

        //verify(logClickUseCase).logClick("key", ip = "127.0.0.1" , UA = userAgentHeaderValue)
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key", "127.0.0.1", null))
            .willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        //verify(logClickUseCase, never()).logClick("key", ip = "127.0.0.1", ua = "")
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
            createQRCodeUseCase.getQRCode("nonexistent")
        ).willAnswer { throw RedirectionNotFound("nonexistent") }
        mockMvc.perform(get("/{id}/qr", "nonexistent"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))
    }
    @Test
    fun `qrCode returns bad request when qr code is not available`() {
        given(
            createQRCodeUseCase.getQRCode("nonexistent")
        ).willAnswer { throw RetryAfterException() }
        mockMvc.perform(get("/{id}/qr", "nonexistent"))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }
    @Test
    fun `qrCode returns qr code image when qr code is available`() {
        val qrCodeBytes = ByteArray(100) // Supongamos que es un QR generado
        given(
            createQRCodeUseCase.getQRCode("existent")
        ).willReturn(qrCodeBytes)
        mockMvc.perform(get("/{id}/qr", "existent"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
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
        Assertions.assertTrue(true)
    }


   @Test
    fun `processCsvFile returns a bad request when CSV format is invalid`() {
        val csvContent = "url1,1,extraColumn\nurl2,0\n"
        val file = MockMultipartFile("file", "test.csv", "text/csv", csvContent.toByteArray())

       mockMvc.perform(
           multipart("/api/bulk")
                .file(file)
               .contentType(MediaType.MULTIPART_FORM_DATA)
        )
       .andExpect(status().isBadRequest())
    }
    @Test
    fun `processCsvFile returns 200 when CSV is empty`() {
        val csvContent = ""
        val file = MockMultipartFile("file", "test.csv", "text/csv", csvContent.toByteArray())

        mockMvc.perform(
            multipart("/api/bulk")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isOk())

    }

    @Test
    fun `return User-Agent info returns info when the key exists`() {
        val key = "someKey"
        val expectedResponse = mapOf("browser" to "Chrome", "os" to "Windows")

        given(userAgentInfoUseCase.getUserAgentInfoByKey(key)).willReturn(expectedResponse)

        mockMvc.perform(get("/api/link/{id}", key))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.browser").value("Chrome"))
                .andExpect(jsonPath("$.os").value("Windows"))
    }

    @Test
    fun `return User-Agent info returns not found when the key does not exist`() {
        val key = "nonexistentKey"
        given(userAgentInfoUseCase.getUserAgentInfoByKey(key)).willAnswer { throw RedirectionNotFound(key) }

        mockMvc.perform(get("/api/link/{id}", key))
                .andDo(print())
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.statusCode").value(404))
    }

}
