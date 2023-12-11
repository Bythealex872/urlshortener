@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery


import org.springframework.web.socket.server.standard.ServerEndpointExporter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.ExecutorChannel
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.integrationFlow
import org.springframework.messaging.MessageChannel
import org.springframework.integration.channel.PublishSubscribeChannel
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.annotation.Gateway
import org.springframework.stereotype.Component
import org.springframework.scheduling.annotation.Scheduled
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import org.springframework.integration.config.EnableIntegration
import org.springframework.scheduling.annotation.EnableScheduling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import java.util.concurrent.Executor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
//import jakarta.websocket.*
import jakarta.websocket.CloseReason.CloseCodes
import jakarta.websocket.server.ServerEndpoint
import es.unizar.urlshortener.core.ShortUrlProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
@Component
open class SpringContext : ApplicationContextAware {

    private lateinit var context: ApplicationContext

    override fun setApplicationContext(context: ApplicationContext) {
        this.context = context
        SpringContext.context = this
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
