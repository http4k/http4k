package org.http4k.lens

import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.lens.ParamMeta.StringParam

typealias HeaderLens<T> = Lens<HttpMessage, T>

object Header : BiDiLensSpec<HttpMessage, String>("header", StringParam,
    LensGet { name, target -> target.headerValues(name).map { it ?: "" } },
    LensSet { name, values, target -> values.fold(target, { m, next -> m.header(name, next) }) }
) {
    object Common {
        val CONTENT_TYPE = map(
            {
                val parts = it.split(";")
                if (parts.size == 2) {
                    val directive = parts[1].split("=")
                    if(directive.size == 2) ContentType(parts[0].trim(), directive[0].trim() to directive[1].trim())
                    else ContentType(parts[0].trim())
                } else ContentType(it.trim())
            },
            ContentType::toHeaderValue).optional("content-type")
    }

    val X_URI_TEMPLATE = optional("x-uri-template")
}