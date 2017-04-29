package org.reekwest.http.lens

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.header

typealias HeaderLens<T> = Lens<Request, T>

object Header : BiDiLensSpec<HttpMessage, String, String>("header",
    Get { name, target -> target.headerValues(name).map { it ?: "" } },
    Set { name, values, target -> values.fold(target, { m, next -> m.header(name, next) }) }
) {
    object Common {
        val X_KONTRACT_ROUTE = optional("x-http.contract-route")
        val CONTENT_TYPE = map(::ContentType, { it.value }).optional("content-type")
    }
}