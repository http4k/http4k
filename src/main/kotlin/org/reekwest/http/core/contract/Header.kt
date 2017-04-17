package org.reekwest.http.core.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.*
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie
import java.nio.ByteBuffer

object Header : LensSpec<HttpMessage, String>(
    object : Locator<HttpMessage, String> {
        override val location = "header"
        override fun get(target: HttpMessage, name: String) = target.headerValues(name)
        override fun set(target: HttpMessage, name: String, values: List<String>) = values.fold(target, { m, next -> m.header(name, next) })
    }.asByteBuffers(),
    ByteBuffer::asString, String::asByteBuffer) {

    object Common {
        val CONTENT_TYPE = map(::ContentType).optional("Content-Type")
    }
}

object Cookies {
    private val a = LensSpec(
        object : Locator<Request, String> {
            override val location = "header"
            override fun get(target: Request, name: String) =
                target.cookie(name)?.let { listOf(it) }?.map(Cookie::toString) ?: emptyList()
            // TODO do something here with request and response (like with body)
            override fun set(target: Request, name: String, values: List<String>) = values.fold(target, { m, next -> m.header("Cookie", next) })
        }.asByteBuffers(),
        { it.asString() }, { it.asByteBuffer() })

}