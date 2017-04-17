package org.reekwest.http.core.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.Request
import org.reekwest.http.core.queries
import org.reekwest.http.core.query
import java.nio.ByteBuffer

object Query : LensSpec<Request, String>(
    (object : Locator<Request, String> {
        override val location = "query"
        override fun get(target: Request, name: String) = target.queries(name)
        override fun set(target: Request, name: String, values: List<String>) = values.fold(target, { m, next -> m.query(name, next) })
    }).asByteBuffers(),
    ByteBuffer::asString, String::asByteBuffer)