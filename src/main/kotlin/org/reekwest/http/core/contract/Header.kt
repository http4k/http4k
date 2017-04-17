package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues

object Header : LensSpec<HttpMessage, String>("header",
    object : NamedLens<HttpMessage, String> {
        override fun get(target: HttpMessage, name: String) = target.headerValues(name)
        override fun set(target: HttpMessage, name: String, values: List<String>) = values.fold(target, { m, next -> m.header(name, next) })
    }.asByteBuffers(),
    ByteBufferStringBiDiMapper) {

    object Common {
        val CONTENT_TYPE = map(::ContentType).optional("Content-Type")
    }
}
