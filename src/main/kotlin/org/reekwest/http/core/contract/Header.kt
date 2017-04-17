package org.reekwest.http.core.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.*
import java.nio.ByteBuffer

object Header : LensSpec<HttpMessage, String>(
    object : Locator<HttpMessage, String> {
        override val location = "header"
        override fun get(target: HttpMessage, name: String) = target.headerValues(name)
        override fun set(target: HttpMessage, name: String, values: List<String>) = values.fold(target, { m, next -> m.header(name, next) })
    }.asByteBuffers(),
    ByteBuffer::asString, String::asByteBuffer) {

    object Common {
        val CONTENT_TYPE = map(::ContentType).optional("Content-Type")
    }
}
