package org.reekwest.http.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.queries
import org.reekwest.http.core.query

object Query : LensSpec<Request, String>("query",
    { name: String ->
        object : Lens<Request, String> {
            override fun invoke(target: Request): List<String> = target.queries(name).map { it ?: "" }
            override fun invoke(values: List<String>, target: Request): Request = values.fold(target, { m, next -> m.query(name, next) })
        }
    }.asByteBuffers(),
    ByteBufferStringBiDiMapper)