package org.reekwest.http.contract

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues

object Header : LensSpec<HttpMessage, String>("header",
    {name: String ->
        object: Lens<HttpMessage, String> {
                override fun invoke(target: HttpMessage): List<String?>?  = target.headerValues(name)
                override fun invoke(values: List<String>, target: HttpMessage)= values.fold(target, { m, next -> m.header(name, next) })
            }
    }.asByteBuffers(),
    ByteBufferStringBiDiMapper) {

    object Common {
        val CONTENT_TYPE = map(::ContentType).optional("Content-Type")
    }
}
