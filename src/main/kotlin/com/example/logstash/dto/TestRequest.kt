package com.example.logstash.dto

data class TestRequest(
    val name: String,
    val password: String? = null
)