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
    /**
     * Metodo que guarda en la base de datos un click de un usuario.
     *
     * @param key the key of the redirection
     * @param ip the ip of the requester
     * @param uastring the user agent of the requester
     */
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

    /**
     * Implementacion del metodo de la interfaz para guardar un click en la base de datos.
     *
     * @param key the key of the redirection
     * @param ip the ip of the requester
     * @param uastring the user agent of the requester
     */
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
