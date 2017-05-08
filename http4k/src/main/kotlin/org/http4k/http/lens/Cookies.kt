package org.http4k.http.lens

import org.http4k.http.core.Request
import org.http4k.http.core.cookie.Cookie
import org.http4k.http.core.cookie.cookie
import org.http4k.http.core.cookie.cookies
import org.http4k.http.lens.ParamMeta.StringParam

object Cookies : BiDiLensSpec<Request, Cookie, Cookie>("cookie", StringParam,
    Get { name, target -> target.cookies().filter { it.name == name } },
    Set { _, values, target -> values.fold(target, { m, (name, value) -> m.cookie(name, value) }) }
)