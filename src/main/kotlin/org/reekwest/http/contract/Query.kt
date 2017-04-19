package org.reekwest.http.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.queries
import org.reekwest.http.core.query

object Query : LensSpec<Request, String>("query",
    { name: String ->
        object : Lens<Request, String> {
            override fun invoke(target: Request): List<String?>? = target.queries(name)
            override fun invoke(values: List<String?>?, target: Request) =
                values?.let { it.fold(target) { memo, next -> next?.let { memo.query(name, it) } ?: memo } } ?: target
        }
    }.asByteBuffers(),
    ByteBufferStringBiDiMapper)