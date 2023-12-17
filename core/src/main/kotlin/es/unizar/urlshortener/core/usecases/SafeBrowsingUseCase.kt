package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.SafeBrowsingService

interface SafeBrowsingUseCase {
    fun urlsAreSafe(urlList: List<String>) : List<String>
    fun urlisSafe(url :String): Boolean
}
class SafeBrowsingUseCaseImpl(
    private val safeBrowsingService: SafeBrowsingService
) : SafeBrowsingUseCase {

    /**
     * Comprueba si una lista de URLs es segura utilizando el servicio de navegación segura.
     *
     * @param urlList Lista de URLs a verificar.
     * @return Lista de URLs seguras.
     */
    override fun urlsAreSafe(urlList: List<String>) : List<String> {
            return safeBrowsingService.urlsAreSafe(urlList)
    }

    /**
     * Comprueba si una URL es segura utilizando el servicio de navegación segura.
     *
     * @param url URL a verificar.
     * @return Booleano que indica si la URL es segura.
     */
    override fun urlisSafe(url: String): Boolean {
        return safeBrowsingService.isSafe(url)
    }
}
