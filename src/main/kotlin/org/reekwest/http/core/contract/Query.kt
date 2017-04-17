package org.reekwest.http.core.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.Request
import org.reekwest.http.core.queries
import org.reekwest.http.core.query
import java.nio.ByteBuffer

object Query : LensSpec<Request, String>(StringLocator("query",
    { request, name -> request.queries(name) },
    { req, name, values -> values.fold(req, { m, next -> m.query(name, next) }) }),
    ByteBuffer::asString, String::asByteBuffer)