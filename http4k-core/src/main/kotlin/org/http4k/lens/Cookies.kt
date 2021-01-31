package org.http4k.lens

import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.cookie.removeCookie
import org.http4k.lens.ParamMeta.StringParam

object Cookies : BiDiLensSpec<Request, Cookie>("cookie", StringParam,
    LensGet { name, target -> target.cookies().filter { it.name == name } },
    LensSet { name, values, target -> values.fold(target.removeCookie(name)) { m, (name, value) -> m.cookie(name, value) } }
)
