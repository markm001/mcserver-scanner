package com.ccat.serverscanner.model

import java.net.InetAddress

/**
 * Retrieve Server Information by Ip & Port
 */
data class ServerPingRequest(
    val address: InetAddress,
    val port: Int
)