package com.example.logstash.common.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class ApiLogEntry(
    val timestamp: String = Instant.now().toString(),
    val level: String = "INFO",
    val method: String,
    val path: String,

    @JsonProperty("status_code")
    val statusCode: Int? = null,

    @JsonProperty("response_time_ms")
    val responseTimeMs: Long? = null,

    @JsonProperty("user_id")
    val userId: String? = null,

    @JsonProperty("ip_address")
    val ipAddress: String? = null,

    @JsonProperty("user_agent")
    val userAgent: String? = null,

    @JsonProperty("request_body")
    val requestBody: String? = null,

    @JsonProperty("response_body")
    val responseBody: String? = null,

    @JsonProperty("error_message")
    val errorMessage: String? = null,

    @JsonProperty("trace_id")
    val traceId: String? = null
)