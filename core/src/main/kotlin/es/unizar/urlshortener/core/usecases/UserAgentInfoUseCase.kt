package es.unizar.urlshortener.core.usecases

import com.blueconic.browscap.Capabilities
import com.blueconic.browscap.UserAgentService
import es.unizar.urlshortener.core.*

private const val CORRECTO = 307

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
        private val shortUrlRepository: ShortUrlRepositoryService,
        private val clickRepository: ClickRepositoryService

) : UserAgentInfoUseCase {
    private val parser = UserAgentService().loadParser()

    override fun getUserAgentInfoByKey(key: String): Map<String, Any>{
        val shortUrl = shortUrlRepository.findByKey(key) ?: throw RedirectionNotFound(key)
        val click = clickRepository.findByKey(key) ?: throw RedirectionNotFound(key)
        // Verifica si la URI recortada no existe
        if(shortUrl.properties.safe == null){ // no valida, posible spam
            throw RetryAfterException()
        }
        if(!shortUrl.properties.safe){ // no operativa
            throw RedirectionForbidden(key)
        }

        return click.let {
            mapOf(
                    "hash" to it.hash,
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
