package org.reekwest.http.contract

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues

object Header : BiDiLensSpec<HttpMessage, String, String>("header",
    GetLens({ name, target -> target.headerValues(name).map { it ?: "" } }, { it }),
    SetLens({ name, values, target -> values.fold(target, { m, next -> m.header(name, next) }) }, { it })
) {
    object Common {
        val CONTENT_TYPE = map(::ContentType, { it.value }).optional("Content-Type")
    }
}