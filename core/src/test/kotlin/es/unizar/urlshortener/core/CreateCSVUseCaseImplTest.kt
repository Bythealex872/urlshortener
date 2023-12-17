package es.unizar.urlshortener.core

import es.unizar.urlshortener.core.usecases.CreateCSVUseCaseImpl
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.net.URI

class CreateCSVUseCaseImplTest {

    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase
    private lateinit var linkToService: LinkToService
    private lateinit var createCSVUseCase: CreateCSVUseCaseImpl

    @BeforeEach
    fun setUp() {
        createShortUrlUseCase = mock()
        linkToService = mock()
        createCSVUseCase = CreateCSVUseCaseImpl(createShortUrlUseCase, linkToService)
    }

    @Test
    fun `processAndBuildCsv should correctly process CSV content`() {
        // Setup mock responses
        whenever(createShortUrlUseCase.create(any(), any(), any())).thenReturn(
                ShortUrl("hash", Redirection("http://example.com"))
        )
        whenever(linkToService.link(any())).thenReturn(URI("http://short.url/hash"))

        // Create a sample CSV input
        val csvInput = "URI,QR\nhttp://example.com,1\nhttp://example.org,0"
        val inputStream = ByteArrayInputStream(csvInput.toByteArray())

        val result = createCSVUseCase.processAndBuildCsv(inputStream, "123.123.123.123")

        // Assertions
        assertNotNull(result)
        assertTrue(result.first.contains("http://example.com"))
        assertTrue(result.first.contains("http://example.org"))
        // Add more assertions as necessary
    }

    @Test
    fun `processAndBuildCsv should throw CSVCouldNotBeProcessed for invalid CSV format`() {
        // Create invalid CSV input
        val invalidCsvInput = "Invalid content"
        val inputStream = ByteArrayInputStream(invalidCsvInput.toByteArray())

        assertThrows<CSVCouldNotBeProcessed> {
            createCSVUseCase.processAndBuildCsv(inputStream, "123.123.123.123")
        }
    }
}
