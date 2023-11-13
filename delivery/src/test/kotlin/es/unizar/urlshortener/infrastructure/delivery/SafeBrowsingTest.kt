package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.usecases.SafeBrowsingImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class SafeBrowsingTest {
    private lateinit var safeBrowsing : SafeBrowsingImpl
    private lateinit var mockRepository: ShortUrlRepositoryService

    @BeforeEach
    fun setup() {
        mockRepository = mock(ShortUrlRepositoryService::class.java)
    }
    @Test
    fun  `safe browser api return the malware testing url `() {
        // Prepara una instancia de SafeBrowsingImp
        safeBrowsing = SafeBrowsingImpl(mockRepository)

        // Prepara una lista de URLs para testear
        val testUrls = listOf("http://www.google.com", "https://malware.testing.google.test/testing/malware/")

        val threadsUrl = safeBrowsing.urlsAreSafe(testUrls)
        assertEquals(threadsUrl.size, 1)
        assertEquals(threadsUrl[0], "https://malware.testing.google.test/testing/malware/")
    }
}

