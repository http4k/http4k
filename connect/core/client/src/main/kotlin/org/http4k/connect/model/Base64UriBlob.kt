package org.http4k.connect.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Base64

class Base64UriBlob private constructor(value: String) : StringValue(value) {
    fun decoded() = String(decodedBytes())
    fun decodedBytes(): ByteArray = Base64.getUrlDecoder().decode(value)
    fun decodedInputStream() = ByteArrayInputStream(decodedBytes())

    companion object : NonBlankStringValueFactory<Base64UriBlob>(::Base64UriBlob) {
        private val encoder = Base64.getUrlEncoder().withoutPadding()

        fun encode(unencoded: String) = encode(unencoded.toByteArray())
        fun encode(unencoded: ByteArray) = Base64UriBlob(encoder.encodeToString(unencoded))
        fun encode(unencoded: InputStream) = encode(unencoded.readAllBytes())
    }
}
