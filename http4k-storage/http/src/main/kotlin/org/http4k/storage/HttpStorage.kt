package org.http4k.storage

import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLens

inline fun <reified T : Any> Storage.Companion.Http(
    crossinline http: HttpHandler,
    bodyLens: BiDiBodyLens<T>
): Storage<T> = object : Storage<T> {

    override fun get(key: String): T? {
        val target = http(Request(GET, "/api/storage/${key}"))
        return if (target.status.successful)
            bodyLens(target) else null
    }

    override fun set(key: String, data: T) {
        http(Request(POST, "/api/storage/${key}").with(bodyLens of data))
    }

    override fun remove(key: String) =
        http(Request(DELETE, "/api/storage/${key}")).status == ACCEPTED

    override fun keySet(keyPrefix: String) =
        http(Request(GET, "/api/storage").query("keyPrefix", keyPrefix)).bodyString().split("\n").toSet()

    override fun removeAll(keyPrefix: String) =
        http(Request(DELETE, "/api/storage").query("keyPrefix", keyPrefix)).status == ACCEPTED
}
