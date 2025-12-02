package com.example.logstash

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LogstashApplication

fun main(args: Array<String>) {
	runApplication<LogstashApplication>(*args)
}
