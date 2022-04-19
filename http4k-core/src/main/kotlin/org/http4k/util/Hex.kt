package org.http4k.util

object Hex {
    fun hex(data: ByteArray): String =
        data.joinToString("") {
            it.toUInt()
                .toString(16)
                .padStart(2, '0')
        }

    fun unhex(data: String): ByteArray {
        check(data.length % 2 == 0) { "Must have an even length" }
        return data.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}
