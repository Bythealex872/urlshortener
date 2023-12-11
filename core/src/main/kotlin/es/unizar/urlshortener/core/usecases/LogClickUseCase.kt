package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService

/**
 * Log that somebody has requested the redirection identified by a key.
 *
 * **Note**: This is an example of functionality.
 */
interface LogClickUseCase {
    fun logClick(key: String, ip:String, ua:String?)
}

/**
 * Implementation of [LogClickUseCase].
 */
class LogClickUseCaseImpl(
    private val clickRepository: ClickRepositoryService,
    private val userAgentInfoUseCase: UserAgentInfoUseCaseImpl

) : LogClickUseCase {
    override fun logClick(key: String, ip:String, uastring:String?){
        val ua = userAgentInfoUseCase.returnUserAgentInfo(uastring)
        val cl = Click(
            hash = key,
            properties = ClickProperties(
                ip = ip,
                browser = ua.browser,
                platform = ua.platform,
            )
        )
        clickRepository.save(cl)
    }
}
