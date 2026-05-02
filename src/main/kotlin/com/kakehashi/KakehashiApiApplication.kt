package com.kakehashi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KakehashiApiApplication

fun main(args: Array<String>) {
    runApplication<KakehashiApiApplication>(*args)
}
