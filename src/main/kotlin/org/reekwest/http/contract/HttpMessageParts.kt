package org.reekwest.http.contract

import org.reekwest.http.core.*
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie
import org.reekwest.http.core.cookie.cookies

typealias QueryLens<T> = Lens<Request, T>

typealias HeaderLens<T> = Lens<Request, T>

typealias PathLens<T> = Lens<String, T>

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
    Get { name, target -> target.cookies().filter { it.name == name } },
    Set { _, values, target -> values.fold(target, { m, (name, value) -> m.cookie(name, value) }) }
)

open class PathSegmentSpec<MID, out OUT>(private val delegate: LensSpec<String, String, OUT>) {
    open fun of(name: String, description: String? = null): PathLens<OUT> = delegate.required(name, description)
    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathSegmentSpec<MID, NEXT> = PathSegmentSpec(delegate.map(nextIn))
}

object Path : PathSegmentSpec<String, String>(LensSpec<String, String, String>("path",
    Get.Companion { _, target -> listOf(target) })) {

    fun int() = map(String::toInt)
    fun fixed(name: String) = of(name)
}
