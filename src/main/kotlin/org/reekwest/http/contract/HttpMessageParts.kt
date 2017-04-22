package org.reekwest.http.contract

import org.reekwest.http.core.*
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie

object Query : BiDiLensSpec<Request, String, String>("query",
    Get { name, target -> target.queries(name).map { it ?: "" } },
    Set { name, values, target -> values.fold(target, { m, next -> m.query(name, next) }) }
)

object Header : BiDiLensSpec<HttpMessage, String, String>("header",
    Get { name, target -> target.headerValues(name).map { it ?: "" } },
    Set { name, values, target -> values.fold(target, { m, next -> m.header(name, next) }) }
) {
    object Common {
        val CONTENT_TYPE = map(::ContentType, { it.value }).optional("Content-Type")
    }
}

object Cookies : BiDiLensSpec<Request, Cookie, Cookie>("cookie",
    Get { name, target -> target.cookie(name)?.let { listOf(it) } ?: emptyList() },
    Set { _, values, target -> values.fold(target, { m, next -> m.header("Cookie", next.toString()) }) }
)