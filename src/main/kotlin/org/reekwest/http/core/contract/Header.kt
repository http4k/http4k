package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues

object Header : LensSpec<HttpMessage, String>("header",
    object : NamedLens<HttpMessage, String> {
        override fun invoke(name: String, target: HttpMessage) = target.headerValues(name)
        override fun invoke(name: String, values: List<String>, target: HttpMessage) = values.fold(target, { m, next -> m.header(name, next) })
    }.asByteBuffers(),
    ByteBufferStringBiDiMapper) {

    object Common {
        val CONTENT_TYPE = map(::ContentType).optional("Content-Type")
    }
}
