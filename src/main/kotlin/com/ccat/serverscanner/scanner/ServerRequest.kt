package com.ccat.serverscanner.scanner

import com.ccat.serverscanner.model.ServerPingRequest
import com.ccat.serverscanner.model.ServerPingResponse
import com.ccat.serverscanner.util.Util
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class ServerRequest constructor(request: ServerPingRequest, requestType: RequestType) {
    companion object {
        const val DEFAULT_PORT: Int = 25565
        private const val DEFAULT_TIMEOUT: Int = 10000
    }

    lateinit var serverResponse: ServerPingResponse
    lateinit var serverStatus: ServerStatus

    constructor(request: ServerPingRequest) : this(request, RequestType.UNDEFINED)

    init {
        when (requestType) {
            RequestType.JSON -> {
                serverStatus = fetchJsonResponse(request.address, request.port)
            }
            RequestType.LEGACY -> {  }
            RequestType.BETA -> {  }
            RequestType.UNDEFINED -> {
                //TODO: attempt all requests in order.
                serverStatus = fetchJsonResponse(request.address, request.port)
            }
        }
    }

    /** 1.7+
     * Serverbound:
     * Handshake Packet ID
     * Protocol version (VarInt) - version that the client plans on using to connect - 1.7+ = 4
     * Server Address (String) - Hostname or IP
     * Server Port (Unsigned Short) - Default:25565
     * state:1 for STATUS / state:2 for LOGIN
     */
    private fun fetchJsonResponse(address: InetAddress, port: Int): ServerStatus {
        val clientSocket = Socket()
        clientSocket.connect(InetSocketAddress(address, port), DEFAULT_TIMEOUT)

        val inStream = DataInputStream(clientSocket.getInputStream())
        val outStream = DataOutputStream(clientSocket.getOutputStream())

        var status:ServerStatus = ServerStatus.FAILURE

        arrayOf(inStream, outStream).use {
            val handshakeOutputStream = ByteArrayOutputStream()
            val handshake = DataOutputStream(handshakeOutputStream)

            handshake.writeByte(0x00)   //Handshake Packet ID
            Util.writeUnsignedVarInt(handshake, 4) //protocol version - 4 for 1.7.5+
            Util.writeUnsignedVarInt(handshake, address.hostAddress.length) //Server-Address length
            handshake.writeBytes(address.hostAddress) //Server-Address string
            handshake.writeShort(port) //Server-Port
            Util.writeUnsignedVarInt(handshake, 1) //State:1 for Status

            //send Handshake Packet:
            Util.writeUnsignedVarInt(outStream, handshakeOutputStream.size()) //prepend Packet size
            outStream.write(handshakeOutputStream.toByteArray()) //write Handshake Packet

            //close unused streams
            handshakeOutputStream.close()
            handshake.close()

            //send Status Request Packet:
            outStream.writeByte(0x01) //Packet size
            outStream.writeByte(0x00) //Packet-Id

            //receive Status Response:
            Util.readUnsignedVarInt(inStream) //Packet size
            val id: Int = Util.readUnsignedVarInt(inStream) //Packet-Id


            if (id == -1) {
                throw IOException("Premature end of stream.")
            }
            if (id != 0x00) {
                throw IOException("Invalid ID for Handshake Packet")
            }

            val length: Int = Util.readUnsignedVarInt(inStream) //JSON-String length
            if (length == -1) {
                throw IOException("Premature end of stream.")
            }
            if (length == 0) {
                throw IOException("Invalid String length.")
            }

            /** Read all bytes from DataInputStream & convert to a String */
            val inBytes = ByteArray(length)
            inStream.readFully(inBytes)
            val jsonResponse = String(inBytes)

            /** Sending a Ping-Request - OBSOLETE!! */
            val currentTimeMillis: Long = System.currentTimeMillis()
            outStream.writeByte(0x09) //Packet size - max 10 bytes for Long Datatype
            outStream.writeByte(0x01) // Ping-Request Packet-ID (1 Byte)
            outStream.writeLong(currentTimeMillis) //Notchian clients use a system-dependent time value. (8 Bytes)

            Util.readUnsignedVarInt(inStream) //Ping Response Packet size
            val pingResponseId = Util.readUnsignedVarInt(inStream) //Ping Response Packet ID
            if (pingResponseId == -1) {
                throw IOException("Premature end of stream.")
            }
            if (pingResponseId != 0x01) {
                throw IOException("Invalid ID for Ping Packet")
            }

            val time: Long = inStream.readLong()

            println("${address.hostName} : Response Time: $currentTimeMillis | $time")

            /** Conversion to usable Response-Object: */
            val serverResponse = Gson().fromJson(jsonResponse, ServerPingResponse::class.java)

            outStream.close()
            inStream.close()
            clientSocket.close()

            this.serverResponse = serverResponse
            status = ServerStatus.SUCCESS
        }

        clientSocket.close()
        return status
    }

    private inline fun <T : Closeable?> Array<T>.use(block: () -> Unit) {
        var exception: Throwable? = null
        try {
            return block()
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            when (exception) {
                null -> forEach { it?.close() }
                else -> forEach {
                    try {
                        it?.close()
                    } catch (closeException: Throwable) {
                        exception.addSuppressed(closeException)
                    }
                }
            }
        }
    }
}