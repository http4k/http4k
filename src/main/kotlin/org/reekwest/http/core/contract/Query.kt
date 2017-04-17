package org.reekwest.http.core.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.queries
import org.reekwest.http.core.query

object Query : LensSpec<Request, String>("query",
    object : TargetFieldLens<Request, String> {
        override fun invoke(name: String, target: Request) = target.queries(name)
        override fun invoke(name: String, values: List<String>, target: Request) = values.fold(target, { m, next -> m.query(name, next) })
    }.asByteBuffers(),
    ByteBufferStringBiDiMapper)