package com.example.logstash.common.filter

import com.example.logstash.common.dto.ApiLogEntry
import com.example.logstash.common.logger.LogstashLogger
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.util.*

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class LoggingFilter(
    private val logstashLogger: LogstashLogger,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {
    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        private val SENSITIVE_FIELDS = setOf("password", "token", "accessToken", "refreshToken", "secret")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val wrappedRequest = ContentCachingRequestWrapper(request, 1024 * 1024)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        val traceId = request.getHeader(TRACE_ID_HEADER) ?: UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        wrappedResponse.setHeader(TRACE_ID_HEADER, traceId)

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse)

            val responseTimeMs = System.currentTimeMillis() - startTime

            logstashLogger.logRequest(
                createLogEntry(
                    request = wrappedRequest,
                    response = wrappedResponse,
                    traceId = traceId,
                    responseTimeMs = responseTimeMs
                )
            )
        } catch (e: Exception) {
            val responseTimeMs = System.currentTimeMillis() - startTime

            logstashLogger.logError(
                createLogEntry(
                    request = wrappedRequest,
                    response = wrappedResponse,
                    traceId = traceId,
                    responseTimeMs = responseTimeMs,
                    errorMessage = e.message
                )
            )
            throw e
        } finally {
            wrappedResponse.copyBodyToResponse()
        }
    }

    private fun createLogEntry(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper,
        traceId: String,
        responseTimeMs: Long,
        errorMessage: String? = null
    ): ApiLogEntry {
        return ApiLogEntry(
            method = request.method,
            path = request.requestURI + (request.queryString?.let { "?$it" } ?: ""),
            statusCode = response.status,
            responseTimeMs = responseTimeMs,
            userId = request.userPrincipal?.name,
            ipAddress = getClientIp(request),
            userAgent = request.getHeader("User-Agent"),
            requestBody = sanitizeBody(getRequestBody(request)),
            responseBody = sanitizeBody(getResponseBody(response)),
            errorMessage = errorMessage,
            traceId = traceId
        )
    }

    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }

    private fun getRequestBody(request: ContentCachingRequestWrapper): String? {
        val content = request.contentAsByteArray
        return if (content.isNotEmpty()) {
            String(content, Charsets.UTF_8)
        } else null
    }

    private fun getResponseBody(response: ContentCachingResponseWrapper): String? {
        val content = response.contentAsByteArray
        return if (content.isNotEmpty()) {
            String(content, Charsets.UTF_8)
        } else null
    }

    private fun sanitizeBody(body: String?): String? {
        if (body.isNullOrBlank()) return null

        return try {
            val jsonNode = objectMapper.readTree(body)
            if (jsonNode.isObject) {
                val mutableNode = jsonNode.deepCopy<com.fasterxml.jackson.databind.node.ObjectNode>()
                SENSITIVE_FIELDS.forEach { field ->
                    if (mutableNode.has(field)) {
                        mutableNode.put(field, "***MASKED***")
                    }
                }
                objectMapper.writeValueAsString(mutableNode)
            } else {
                body
            }
        } catch (e: Exception) {
            body
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        // 헬스체크, actuator 등 제외
        return path.startsWith("/actuator") || path == "/favicon.ico"
    }
}