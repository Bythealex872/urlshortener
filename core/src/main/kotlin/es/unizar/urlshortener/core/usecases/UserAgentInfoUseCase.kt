package es.unizar.urlshortener.core.usecases

import com.blueconic.browscap.Capabilities
import com.blueconic.browscap.UserAgentService
import es.unizar.urlshortener.core.RedirectionForbidden
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.UserAgent
import es.unizar.urlshortener.core.RetryAfterException

private const val RETRYAFTER = 403

/**
 * Given a key returns user agent information.
 */
interface UserAgentInfoUseCase {
    fun getUserAgentInfoByKey(key: String): Map<String, Any>
    fun returnUserAgentInfo(uaString: String?): UserAgent?

}

/**
 * Implementation of [UserAgentInfoUseCase].
 */
class UserAgentInfoUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService
) : UserAgentInfoUseCase {
    private val parser = UserAgentService().loadParser()

    override fun getUserAgentInfoByKey(key: String): Map<String, Any>{
        val shortUrl = shortUrlRepository.findByKey(key) ?: throw RedirectionNotFound(key)
        // Verifica si la URI recortada no existe
        if(!shortUrl.properties.safe){ // no valida, posible spam
            throw RedirectionForbidden(key)
        }
        if(shortUrl.redirection.mode == RETRYAFTER){ // no operativa
            throw RetryAfterException()
        }
        return shortUrl.let {
            mapOf(
                    "id" to key,
                    "hash" to it.hash,
                    "redirection" to it.redirection.target,
                    "created" to it.created.toString(),
                    "properties" to it.properties
            )
        }
    }

    override fun returnUserAgentInfo(uaString: String?): UserAgent {
        val capabilities: Capabilities = parser.parse(uaString)
        val browser = capabilities.browser
        val platform = capabilities.platform
        return UserAgent(browser, platform)
    }

}
