package com.ccat.serverscanner.scanner

enum class RequestType {
    LEGACY, JSON, BETA, UNDEFINED
}

enum class ServerStatus {
    SUCCESS, FAILURE, TIMEOUT
}