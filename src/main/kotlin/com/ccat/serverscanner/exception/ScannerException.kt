package com.ccat.serverscanner.exception

import org.springframework.http.HttpStatus

class ScannerException(
    override val message: String,
    val statusCode: HttpStatus
): RuntimeException(message)

class InvalidPortException(
    override val message: String,
    val statusCode: HttpStatus = HttpStatus.BAD_REQUEST
): RuntimeException(message)

class InvalidIpException(
    override val message: String,
    val statusCode: HttpStatus = HttpStatus.BAD_REQUEST
): RuntimeException(message)