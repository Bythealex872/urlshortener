package es.unizar.urlshortener.infrastructure.delivery

import org.springframework.stereotype.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimiter {
    private val logger: Logger = LoggerFactory.getLogger(RateLimiter::class.java)

    private val requestTimestamps = ConcurrentHashMap<String, MutableList<Instant>>()
    private val maxRequests = 10 // Máximo de solicitudes permitidas
    private val timeWindow = 60000L // Ventana de tiempo en milisegundos (ejemplo: 60 segundos)

    fun isLimitExceeded(clientId: String): Boolean {
        val currentTime = Instant.now()
        val timestamps = requestTimestamps.getOrPut(clientId) { mutableListOf() }
        logger.info("$clientId: ${timestamps.size}")

        // Eliminar marcas de tiempo antiguas que ya no están dentro de la ventana de tiempo
        timestamps.removeIf { timestamp -> 
            timestamp.plusMillis(timeWindow).isBefore(currentTime)
        }

        // Verificar si se excede el límite de tasa
        if (timestamps.size >= maxRequests) {
            return true
        }

        // Agregar la marca de tiempo actual
        timestamps.add(currentTime)
        return false
    }

    fun timeToNextRequest(clientId: String): Long {
        val currentTime = Instant.now()
        requestTimestamps[clientId]?.let { timestamps ->
            // Encuentra la marca de tiempo más antigua que aún está en la ventana de tiempo
            val oldestTimestamp = timestamps.filter { 
                it.plusMillis(timeWindow).isAfter(currentTime)
            }.minOrNull()

            return oldestTimestamp?.let { 
                Duration.between(currentTime, it.plusMillis(timeWindow)).toMillis()
            } ?: 0
        }

        return 0 // Si no hay marcas de tiempo, el cliente puede hacer una solicitud de inmediato
    }

}