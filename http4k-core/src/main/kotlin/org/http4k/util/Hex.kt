package org.http4k.util

object Hex {
    fun hex(data: ByteArray): String = data.joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }
    fun unhex(data: String): ByteArray {
        val result = ByteArray(data.length / 2)
        for (idx in result.indices) {
            val srcIdx = idx * 2
            val high = data[srcIdx].toString().toInt(16) shl 4
            val low = data[srcIdx + 1].toString().toInt(16)
            result[idx] = (high or low).toByte()
        }

        return result
    }
}
