package com.ccat.serverscanner.util

import java.io.DataInputStream
import java.io.DataOutputStream

object Util {
    private const val SEGMENT_BITS = 0x7F
    private const val CONTINUE_BIT = 0x80
    
    /*
    https://wiki.vg/VarInt_And_VarLong
     */

    fun writeUnsignedVarInt(out:DataOutputStream, inVal: Int) {
        var value:Int = inVal
        var remaining = value.shr(7)

        while(remaining != 0) {
            out.writeByte(value and SEGMENT_BITS or CONTINUE_BIT)
            value = remaining
            remaining = remaining.shr(7)
        }

        out.writeByte(value and SEGMENT_BITS)
    }

    fun readUnsignedVarInt(input: DataInputStream): Int {
        var value = 0
        var position = 0
        var currentByte: Byte
        while (true) {
            currentByte = input.readByte()
            value = value or (currentByte.toInt() and SEGMENT_BITS shl position)
            if (currentByte.toInt() and CONTINUE_BIT == 0) break
            position += 7
            if (position >= 32) throw RuntimeException("VarInt too big")
        }
        return value
    }
}