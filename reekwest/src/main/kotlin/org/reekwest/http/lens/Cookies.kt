package org.reekwest.http.lens

import org.reekwest.http.core.Request
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie
import org.reekwest.http.core.cookie.cookies

object Cookies : BiDiLensSpec<Request, Cookie, Cookie>("cookie",
    Get { name, target -> target.cookies().filter { it.name == name } },
    Set { _, values, target -> values.fold(target, { m, (name, value) -> m.cookie(name, value) }) }
)