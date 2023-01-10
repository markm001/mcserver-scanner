package com.ccat.serverscanner.controller

import com.ccat.serverscanner.exception.InvalidIpException
import com.ccat.serverscanner.exception.InvalidPortException
import com.ccat.serverscanner.model.ServerPingRequest
import com.ccat.serverscanner.model.ServerPingResponse
import com.ccat.serverscanner.model.service.ScannerService
import com.ccat.serverscanner.scanner.RequestType
import com.ccat.serverscanner.scanner.ServerRequest.Companion.DEFAULT_PORT
import org.apache.commons.net.util.SubnetUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

@RestController
class HomeController(
   private val scannerService: ScannerService
) {
    @GetMapping("/server/{address}/{netmask}")
    fun scanSubnet(@PathVariable address:String, @PathVariable netmask:Int) {
        try {
            val ip: InetAddress = Inet4Address.getByName(address)

            val addresses:Array<String> = SubnetUtils("${ip.hostAddress}/$netmask").info.allAddresses

            scannerService.scanAddressSubnet(addresses)
        } catch (e:Exception) { //TODO: MOVE TO CONTROLLER ADVICE
            e.printStackTrace()
        }
    }

    @GetMapping("/server/{address}:{port}")
    fun getServerInformationByAddressAndPort(@PathVariable address: String, @PathVariable port: Int)
    : ResponseEntity<ServerPingResponse?> {
        try {
            val ip: InetAddress = Inet4Address.getByName(address)

            if(port != DEFAULT_PORT) {
                throw InvalidPortException("The requested Port:$port is not valid")
            }

            val serverPingRequest = ServerPingRequest(ip, port)


            val response:Optional<ServerPingResponse> = scannerService.scanAddress(serverPingRequest, RequestType.JSON)
            return if(response.isPresent) {
                ResponseEntity.ok(response.get())
            } else {
                ResponseEntity.notFound().build()
            }

        } catch (e:UnknownHostException) { //TODO: MOVE TO CONTROLLER ADVICE
            throw InvalidIpException("The requested IP:$address is not valid")
        }
    }
}