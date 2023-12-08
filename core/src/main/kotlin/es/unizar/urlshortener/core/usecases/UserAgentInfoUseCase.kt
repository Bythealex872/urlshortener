package es.unizar.urlshortener.core.usecases

import com.blueconic.browscap.Capabilities
import com.blueconic.browscap.UserAgentService
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.UserAgent

/**
 * Given a key returns user agent information.
 */
interface UserAgentInfoUseCase {
    fun getUserAgentInfoByKey(key: String): Map<String, Any>?
    fun returnUserAgentInfo(UAstring: String): UserAgent?

}

/**
 * Implementation of [UserAgentInfoUseCase].
 */
class UserAgentInfoUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService
) : UserAgentInfoUseCase {
    private val parser = UserAgentService().loadParser()

    override fun getUserAgentInfoByKey(key: String): Map<String, Any>? {
        val shortUrl = shortUrlRepository.findByKey(key)

        return shortUrl?.let {
            mapOf(
                    "id" to key,
                    "hash" to it.hash,
                    "redirection" to it.redirection.target,
                    "created" to it.created.toString(),
                    "properties" to it.properties
            )
        }
    }

    override fun returnUserAgentInfo(UAstring: String): UserAgent? {
        val capabilities: Capabilities = parser.parse(UAstring)
        val browser = capabilities.browser
        val platform = capabilities.platform
        return UserAgent(browser, platform)
    }

}
