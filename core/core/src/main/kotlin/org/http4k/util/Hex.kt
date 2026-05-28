package org.http4k.util

object Hex {
    fun hex(data: ByteArray): String = data.joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }

    fun unhex(data: String): ByteArray {
        require(data.length % 2 == 0) { "Must have an even length" }
        require(data.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) { "Must be a valid hex string" }
        return data.chunked(2)
            .map { nextByte -> nextByte.toInt(16).toByte() }
            .toByteArray()
    }
}
