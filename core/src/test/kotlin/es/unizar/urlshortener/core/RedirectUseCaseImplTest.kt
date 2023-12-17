package es.unizar.urlshortener.core

import es.unizar.urlshortener.core.usecases.RedirectUseCaseImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RedirectUseCaseImplTest {

    private lateinit var shortUrlRepository: ShortUrlRepositoryService
    private lateinit var uaService: UserAgentRequestService
    private lateinit var redirectUseCase: RedirectUseCaseImpl

    @BeforeEach
    fun setUp() {
        shortUrlRepository = mock()
        uaService = mock()
        redirectUseCase = RedirectUseCaseImpl(shortUrlRepository, uaService)
    }

    @Test
    fun `redirectTo should return valid Redirection when URL is found and safe`() {
        val key = "validKey"
        val redirection = Redirection("http://example.com")
        val shortUrl = ShortUrl(key, redirection, properties = ShortUrlProperties(safe = true))

        whenever(shortUrlRepository.findByKey(key)).thenReturn(shortUrl)

        val result = redirectUseCase.redirectTo(key, "123.123.123.123", "Mozilla")

        assertEquals(redirection, result)
        verify(uaService).sendUserAgentMessage(Triple(key, "123.123.123.123", "Mozilla"))
    }

    @Test
    fun `redirectTo should throw RedirectionNotFound when URL is not found`() {
        val key = "invalidKey"

        whenever(shortUrlRepository.findByKey(key)).thenReturn(null)

        assertThrows<RedirectionNotFound> {
            redirectUseCase.redirectTo(key, "123.123.123.123", "Mozilla")
        }
    }

    @Test
    fun `redirectTo should throw RetryAfterException when URL is not validated yet`() {
        val key = "unvalidatedKey"
        val shortUrl = ShortUrl(key, Redirection("http://example.com"), properties = ShortUrlProperties(safe = null))

        whenever(shortUrlRepository.findByKey(key)).thenReturn(shortUrl)

        assertThrows<RetryAfterException> {
            redirectUseCase.redirectTo(key, "123.123.123.123", "Mozilla")
        }
    }

    @Test
    fun `redirectTo should throw RedirectionForbidden when URL is not safe`() {
        val key = "unsafeKey"
        val shortUrl = ShortUrl(key, Redirection("http://example.com"), properties = ShortUrlProperties(safe = false))

        whenever(shortUrlRepository.findByKey(key)).thenReturn(shortUrl)

        assertThrows<RedirectionForbidden> {
            redirectUseCase.redirectTo(key, "123.123.123.123", "Mozilla")
        }
    }

}
