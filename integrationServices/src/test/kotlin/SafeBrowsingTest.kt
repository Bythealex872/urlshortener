package es.unizar.urlshortener.integrationServices

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SafeBrowsingTest {
    private lateinit var safeBrowsing : SafeBrowsingServiceImpl


    @BeforeEach
    fun setup() {

    }
    @Test
    fun  `safe browser api return the malware testing url `() {
        // Prepara una instancia de SafeBrowsingImp
        safeBrowsing = SafeBrowsingServiceImpl()

        // Prepara una lista de URLs para testear
        val testUrls = listOf("http://www.google.com", "https://malware.testing.google.test/testing/malware/")

        val threadsUrl = safeBrowsing.urlsAreSafe(testUrls)
        println(threadsUrl)
        assertEquals(threadsUrl.size, 1)
        assertEquals(threadsUrl[0], "https://malware.testing.google.test/testing/malware/")
    }
}

