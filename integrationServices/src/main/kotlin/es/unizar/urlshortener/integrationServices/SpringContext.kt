@file:Suppress("WildcardImport")

package es.unizar.urlshortener.integrationServices


import org.springframework.stereotype.Component
//import jakarta.websocket.*
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
/**
 * Clase que proporciona acceso al contexto de la aplicación Spring.
 *
 * Implementa la interfaz [ApplicationContextAware] para recibir el contexto de la aplicación al inicializarse.
 * Permite obtener instancias de beans gestionados por Spring.
 */
@Component
open class SpringContext : ApplicationContextAware {

    private lateinit var context: ApplicationContext
    /**
     * Método llamado por el contenedor Spring para proporcionar el contexto de la aplicación.
     *
     * @param context El contexto de la aplicación Spring.
     */
    override fun setApplicationContext(context: ApplicationContext) {
        this.context = context
        Companion.context = this
    }
    /**
     * Obtiene una instancia del bean especificado por su clase.
     *
     * @param beanClass La clase del bean que se desea obtener.
     * @return La instancia del bean.
     */
    fun <T> getBean(beanClass: Class<T>): T {
        return context.getBean(beanClass)
    }

    /**
     * Obtiene una instancia del bean especificado por su clase utilizando el contexto de la aplicación.
     *
     * @param beanClass La clase del bean que se desea obtener.
     * @return La instancia del bean.
     */
    companion object : DI {
        lateinit var context: SpringContext
        /**
         * Obtiene una instancia del bean especificado por su clase utilizando el contexto de la aplicación.
         *
         * @param beanClass La clase del bean que se desea obtener.
         * @return La instancia del bean.
         */
        override fun <T> getBean(beanClass: Class<T>): T {
            return context.getBean(beanClass)
        }
    }
}

interface DI {
    /**
     * Obtiene una instancia del bean especificado por su clase.
     *
     * @param beanClass La clase del bean que se desea obtener.
     * @return La instancia del bean.
     */
    fun <T> getBean(beanClass: Class<T>): T
}
