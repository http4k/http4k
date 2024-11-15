package org.http4k.connect.storage

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.lens.BiDiBodyLens

inline fun <reified T : Any> Storage.Companion.Http(
    crossinline http: HttpHandler,
    bodyLens: BiDiBodyLens<T> = Body.auto<T>().toLens()
): Storage<T> = object : Storage<T> {

    override fun get(key: String): T? {
        val target = http(Request(Method.GET, "/api/storage/${key}"))
        return if (target.status.successful)
            bodyLens(target) else null
    }

    override fun set(key: String, data: T) {
        http(Request(Method.POST, "/api/storage/${key}").with(bodyLens of data))
    }

    override fun remove(key: String): Boolean {
        val result = http(Request(Method.DELETE, "/api/storage/${key}"))
        return result.status == Status.ACCEPTED
    }

    override fun keySet(keyPrefix: String): Set<String> {
        val result = http(Request(Method.GET, "/api/storage").query("keyPrefix", keyPrefix))
        return result.bodyString().split("\n").toSet()
    }

    override fun removeAll(keyPrefix: String): Boolean {
        val result = http(Request(Method.DELETE, "/api/storage").query("keyPrefix", keyPrefix))
        return result.status == Status.ACCEPTED
    }
}
