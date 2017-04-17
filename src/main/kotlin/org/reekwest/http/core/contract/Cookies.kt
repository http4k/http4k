package org.reekwest.http.core.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.Request
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie
import org.reekwest.http.core.header
import java.nio.ByteBuffer

object Cookies {
    private val delegate = LensSpec(
        object : Locator<Request, String> {
            override val location = "cookie"
            override fun get(target: Request, name: String) =
                target.cookie(name)?.let { listOf(it) }?.map(Cookie::toString) ?: emptyList()
            override fun set(target: Request, name: String, values: List<String>) = values.fold(target, { m, next -> m.header("Cookie", next) })
        }.asByteBuffers(),
        ByteBuffer::asString, String::asByteBuffer)
        .map({Cookie("name", "value")}, {it.toString()})

    fun optional(name: String) = delegate.optional(name)
}