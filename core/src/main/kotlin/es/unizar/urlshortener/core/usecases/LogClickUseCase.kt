package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Log that somebody has requested the redirection identified by a key.
 *
 * **Note**: This is an example of functionality.
 */
interface LogClickUseCase {
    fun logClick(key: String, ip:String, uastring:String?)
}

/**
 * Implementation of [LogClickUseCase].
 */
class LogClickUseCaseImpl(
    private val clickRepository: ClickRepositoryService,
    private val userAgentInfoUseCase: UserAgentInfoUseCaseImpl

) : LogClickUseCase {
    private val logger: Logger = LoggerFactory.getLogger(LogClickUseCaseImpl::class.java)

    override fun logClick(key: String, ip: String, uastring:String?){
        logger.info("Guardando click de $key en la base de datos")
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
