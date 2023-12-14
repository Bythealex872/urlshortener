@file:Suppress("WildcardImport")

package es.unizar.urlshortener.integrationServices


import org.springframework.stereotype.Component
//import jakarta.websocket.*
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
@Component
open class SpringContext : ApplicationContextAware {

    private lateinit var context: ApplicationContext

    override fun setApplicationContext(context: ApplicationContext) {
        this.context = context
        Companion.context = this
    }

    fun <T> getBean(beanClass: Class<T>): T {
        return context.getBean(beanClass)
    }

    companion object : DI {
        lateinit var context: SpringContext

        override fun <T> getBean(beanClass: Class<T>): T {
            return context.getBean(beanClass)
        }
    }
}

interface DI {
    fun <T> getBean(beanClass: Class<T>): T
}
