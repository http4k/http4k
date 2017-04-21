package org.reekwest.http.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie
import org.reekwest.http.core.header

object Cookies : BiDiLensSpec<Request, Cookie, Cookie>("cookie",
    Get({ name, target -> target.cookie(name)?.let { listOf(it) } ?: emptyList() }, { it }),
    Set({ _, values, target -> values.fold(target, { m, next -> m.header("Cookie", next.toString()) }) }, { it })
)