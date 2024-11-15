package org.http4k.connect.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.base64Decoded
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Base64

class Base64Blob private constructor(value: String) : StringValue(value) {
    fun decoded() = value.base64Decoded()
    fun decodedBytes(): ByteArray = Base64.getDecoder().decode(value)
    fun decodedInputStream() = ByteArrayInputStream(decodedBytes())

    companion object : NonBlankStringValueFactory<Base64Blob>(::Base64Blob) {
        fun encode(unencoded: String) = encode(unencoded.toByteArray())
        fun encode(unencoded: ByteArray) = Base64Blob(Base64.getEncoder().encodeToString(unencoded))
        fun encode(unencoded: InputStream) = encode(unencoded.readAllBytes())
    }
}
