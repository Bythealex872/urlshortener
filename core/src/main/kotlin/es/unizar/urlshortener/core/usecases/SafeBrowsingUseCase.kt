package es.unizar.urlshortener.core.usecases
import es.unizar.urlshortener.core.SafeBrowsingService

/**
 * TO DO FOR WORKERS?
 */


interface SafeBrowsingUseCase {
    fun urlsAreSafe(urlList: List<String>) : List<String>
    fun urlisSafe(url :String): Boolean

}
class SafeBrowsingUseCaseImpl(
    private val safeBrowsingService: SafeBrowsingService
) : SafeBrowsingUseCase {

    override fun urlsAreSafe(urlList: List<String>) : List<String> {
            return safeBrowsingService.urlsAreSafe(urlList)
    }

    override fun urlisSafe(url: String): Boolean {
        return safeBrowsingService.isSafe(url)
    }




}

