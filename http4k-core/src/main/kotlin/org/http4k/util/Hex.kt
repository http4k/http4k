package org.http4k.util

object Hex {
    fun hex(data: ByteArray): String = data.joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }
}
