package es.unizar.urlshortener.core

import com.blueconic.browscap.Capabilities
import com.blueconic.browscap.UserAgentParser
import com.blueconic.browscap.UserAgentService
import es.unizar.urlshortener.core.usecases.UserAgentInfoUseCaseImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UserAgentInfoUseCaseImplTest {

    private lateinit var userAgentService: UserAgentService
    private lateinit var userAgentParser: UserAgentParser
    private lateinit var shortUrlRepository: ShortUrlRepositoryService
    private lateinit var clickRepository: ClickRepositoryService
    private lateinit var userAgentInfoUseCase: UserAgentInfoUseCaseImpl

    @BeforeEach
    fun setUp() {
        userAgentService = mock()
        userAgentParser = mock()
        whenever(userAgentService.loadParser()).thenReturn(userAgentParser)
        shortUrlRepository = mock()
        clickRepository = mock()
        userAgentInfoUseCase = UserAgentInfoUseCaseImpl(shortUrlRepository, clickRepository)
    }

    @Test
    fun `getUserAgentInfoByKey should return valid information when URL and Click are found and safe`() {
        val key = "validKey"
        val shortUrl = ShortUrl(key, Redirection("http://example.com"), properties = ShortUrlProperties(safe = true))
        val click = Click(key, ClickProperties(browser = "Chrome", platform = "Windows"))

        whenever(shortUrlRepository.findByKey(key)).thenReturn(shortUrl)
        whenever(clickRepository.findByKey(key)).thenReturn(click)

        val result = userAgentInfoUseCase.getUserAgentInfoByKey(key)

        assertNotNull(result)
        val properties = result["Properties"] as ClickProperties
        assertEquals("Chrome", properties.browser)
        assertEquals("Windows", properties.platform)
    }

    @Test
    fun `getUserAgentInfoByKey should throw RedirectionNotFound when URL is not found`() {
        val key = "invalidKey"

        whenever(shortUrlRepository.findByKey(key)).thenReturn(null)

        assertThrows<RedirectionNotFound> {
            userAgentInfoUseCase.getUserAgentInfoByKey(key)
        }
    }

}
