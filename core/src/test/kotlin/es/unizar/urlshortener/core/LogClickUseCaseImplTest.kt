package es.unizar.urlshortener.core

import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCaseImpl
import es.unizar.urlshortener.core.usecases.UserAgentInfoUseCaseImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LogClickUseCaseImplTest {

    private lateinit var clickRepository: ClickRepositoryService
    private lateinit var userAgentInfoUseCase: UserAgentInfoUseCaseImpl
    private lateinit var logClickUseCase: LogClickUseCase

    @BeforeEach
    fun setUp() {
        clickRepository = mock()
        userAgentInfoUseCase = mock()
        logClickUseCase = LogClickUseCaseImpl(clickRepository, userAgentInfoUseCase)
    }

    @Test
    fun `logClick should save a click with the correct information`() {
        val key = "testKey"
        val ip = "127.0.0.1"
        val uaString = "Mozilla/5.0"
        val userAgent = UserAgent("Chrome", "Windows")

        whenever(userAgentInfoUseCase.returnUserAgentInfo(uaString)).thenReturn(userAgent)

        logClickUseCase.logClick(key, ip, uaString)

        verify(clickRepository).save(check {
            assertEquals(key, it.hash)
            assertEquals(ip, it.properties.ip)
            assertEquals(userAgent.browser, it.properties.browser)
            assertEquals(userAgent.platform, it.properties.platform)
        })
    }
}
