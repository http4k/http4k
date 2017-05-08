package org.http4k.http.lens

import org.http4k.http.core.ContentType
import org.http4k.http.core.HttpMessage
import org.http4k.http.core.Request
import org.http4k.http.lens.ParamMeta.StringParam

typealias HeaderLens<T> = Lens<Request, T>

object Header : BiDiLensSpec<HttpMessage, String, String>("header", StringParam,
    Get { name, target -> target.headerValues(name).map { it ?: "" } },
    Set { name, values, target -> values.fold(target, { m, next -> m.header(name, next) }) }
) {
    object Common {
        val CONTENT_TYPE = map(::ContentType, { it.value }).optional("content-type")
    }
}