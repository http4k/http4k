package org.http4k.lens

import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.lens.ParamMeta.StringParam

typealias HeaderLens<T> = Lens<HttpMessage, T>

object Header : BiDiLensSpec<HttpMessage, String, String>("header", StringParam,
    LensGet { name, target -> target.headerValues(name).map { it ?: "" } },
    LensSet { name, values, target -> values.fold(target, { m, next -> m.header(name, next) }) }
) {
    object Common {
        val CONTENT_TYPE = map({ ContentType(it.takeWhile { it != ';' }) }, { it.value }).optional("content-type")
    }

    val X_URI_TEMPLATE = optional("x-uri-template")
}