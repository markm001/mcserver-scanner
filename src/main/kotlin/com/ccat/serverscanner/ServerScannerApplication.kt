package com.ccat.serverscanner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ServerScannerApplication

fun main(args: Array<String>) {
	runApplication<ServerScannerApplication>(*args)
}
