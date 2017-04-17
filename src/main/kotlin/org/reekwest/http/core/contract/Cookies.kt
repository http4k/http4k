package org.reekwest.http.core.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie
import org.reekwest.http.core.header

object Cookies {
    private val delegate = LensSpec("cookie",
        object : NamedLens<Request, String> {
            override fun invoke(name: String, target: Request) = target.cookie(name)?.let { listOf(it) }?.map(Cookie::toString) ?: emptyList()
            override fun invoke(name: String, values: List<String>, target: Request) = values.fold(target, { m, next -> m.header("Cookie", next) })
        }.asByteBuffers(),
        ByteBufferStringBiDiMapper.map({ Cookie("name", "value") }, { it.toString() })
    )

    fun optional(name: String) = delegate.optional(name)
}