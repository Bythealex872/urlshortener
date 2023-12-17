@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core

import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCaseImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class CreateShortUrlUseCaseImplTests {

    private lateinit var shortUrlRepository: ShortUrlRepositoryService
    private lateinit var validatorService: ValidatorService
    private lateinit var hashService: HashService
    private lateinit var qrRequestService: QRRequestService
    private lateinit var safeBrowsingRequestService: SafeBrowsingRequestService
    private lateinit var linkToService: LinkToService
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCaseImpl

    @BeforeEach
    fun setUp() {
        shortUrlRepository = mock()
        validatorService = mock()
        hashService = mock()
        qrRequestService = mock()
        safeBrowsingRequestService = mock()
        linkToService = mock()
        createShortUrlUseCase = CreateShortUrlUseCaseImpl(
                shortUrlRepository,
                validatorService,
                hashService,
                qrRequestService,
                safeBrowsingRequestService,
                linkToService
        )
    }

    @Test
    fun `create should throw InvalidUrlException for invalid URL`() {
        whenever(validatorService.isValid(any())).thenReturn(false)

        assertThrows<InvalidUrlException> {
            createShortUrlUseCase.create("http://invalid.url", false, ShortUrlProperties())
        }
    }

    @Test
    fun `create should return existing short URL if already exists`() {
        whenever(validatorService.isValid(any())).thenReturn(true)
        whenever(hashService.hasUrl(any())).thenReturn("hash")
        whenever(shortUrlRepository.findByKey(any())).thenReturn(
                ShortUrl("hash", Redirection("http://valid.url"))
        )

        val result = createShortUrlUseCase.create("http://valid.url", false, ShortUrlProperties())

        assertNotNull(result)
        assertEquals("hash", result.hash)
    }

    @Test
    fun `create should create new short URL if not exists`() {
        whenever(validatorService.isValid(any())).thenReturn(true)
        whenever(hashService.hasUrl(any())).thenReturn("newHash")
        whenever(shortUrlRepository.findByKey(any())).thenReturn(null)
        whenever(shortUrlRepository.save(any())).thenAnswer { it.arguments[0] as ShortUrl }

        val result = createShortUrlUseCase.create("http://new.url", false, ShortUrlProperties())

        assertNotNull(result)
        assertEquals("newHash", result.hash)
        verify(safeBrowsingRequestService).sendSafeBrowsingMessage(Pair("newHash", "http://new.url"))
    }

}
