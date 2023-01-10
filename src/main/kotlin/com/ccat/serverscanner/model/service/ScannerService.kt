package com.ccat.serverscanner.model.service

import com.ccat.serverscanner.model.ServerPingRequest
import com.ccat.serverscanner.model.ServerPingResponse
import com.ccat.serverscanner.scanner.RequestType
import com.ccat.serverscanner.scanner.ServerRequest
import com.ccat.serverscanner.scanner.ServerStatus
import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import java.net.Inet4Address
import java.util.*

@Service
class ScannerService {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun scanAddressSubnet(addresses: Array<String>) {
        runBlocking(SupervisorJob() + Dispatchers.IO.limitedParallelism(500)) {
            addresses.map {
                ServerPingRequest(Inet4Address.getByName(it), ServerRequest.DEFAULT_PORT)
            }.map {
                    async { ServerRequest(it, RequestType.JSON) }
            }
                .toList()
                .awaitAll()
        }

        println(addresses)
    }

    fun scanAddress(request:ServerPingRequest, requestType:RequestType?): Optional<ServerPingResponse> {
        val scanner: ServerRequest = if(requestType == null) {
            ServerRequest(request)
        } else {
            ServerRequest(request, requestType)
        }

        if(scanner.serverStatus == ServerStatus.SUCCESS) {
            return Optional.of(scanner.serverResponse)
        }

        return Optional.empty()
    }
}