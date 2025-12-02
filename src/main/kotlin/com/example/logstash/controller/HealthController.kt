package com.example.logstash.controller

import com.example.logstash.dto.TestRequest
import com.example.logstash.dto.TestResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {

    @GetMapping("/health")
    fun getHealth(): Map<String, String> {
        return mapOf("status" to "ok")
    }

        @PostMapping("/test")
    fun test(@RequestBody body: TestRequest): TestResponse {
        return TestResponse(
            message = "Received: ${body.name}",
            timestamp = System.currentTimeMillis()
        )
    }
}