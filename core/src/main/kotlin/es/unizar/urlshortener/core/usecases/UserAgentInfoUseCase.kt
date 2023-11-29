package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrlRepositoryService

/**
 * Given a key returns user agent information.
 */
interface UserAgentInfoUseCase {
    fun getUserAgentInfoByKey(key: String): Map<String, Any>?
}

/**
 * Implementation of [UserAgentInfoUseCase].
 */
class UserAgentInfoUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService
) : UserAgentInfoUseCase {

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
}
