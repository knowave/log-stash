package com.example.logstash.common.logger

import com.example.logstash.common.dto.ApiLogEntry
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

@Component
class LogstashLogger(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${logstash.host}")
    private lateinit var host: String

    @Value("\${logstash.port}")
    private var port: Int = 5000

    @Value("\${logstash.enabled}")
    private var enabled: Boolean = true

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private val isConnected = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null

    @PostConstruct
    fun init() {
        if (enabled) {
            connect()
        }
    }

    private fun connect() {
        scope.launch {
            try {
                socket = Socket(host, port)
                writer = PrintWriter(socket!!.getOutputStream(), true)
                isConnected.set(true)
                logger.info("Connected to Logstash at $host:$port")
            } catch (e: Exception) {
                logger.error("Failed to connect to Logstash: ${e.message}")
                isConnected.set(false)
                scheduleReconnect()
            }
        }
    }

    private fun disconnect() {
        try {
            writer?.close()
            socket?.close()
            isConnected.set(false)
        } catch (e: Exception) {
            logger.error("Error closing Logstash connection: ${e.message}")
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob == null || reconnectJob?.isActive != true) {
            reconnectJob = scope.launch {
                while (!isConnected.get()) {
                    delay(5000)
                    logger.info("Attempting to reconnect to Logstash...")
                    connect()
                }
            }
        }
    }

    fun log(entry: ApiLogEntry) {
        if (!enabled) return

        scope.launch {
            try {
                val message = objectMapper.writeValueAsString(entry)
                if (isConnected.get() && writer != null) {
                    writer?.println(message)
                    if (writer?.checkError() == true) {
                        isConnected.set(false)
                        scheduleReconnect()
                    }
                } else {
                    logger.debug("[Logstash Offline] $message")
                }
            } catch (e: Exception) {
                logger.error("Failed to send log to Logstash: ${e.message}")
                isConnected.set(false)
                scheduleReconnect()
            }
        }
    }

    fun logRequest(entry: ApiLogEntry) {
        log(entry.copy(level = "INFO"))
    }

    fun logError(entry: ApiLogEntry) {
        log(entry.copy(level = "ERROR"))
    }
}