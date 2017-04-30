package org.reekwest.http.core.cookie

import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import java.util.concurrent.ConcurrentHashMap

interface CookieStorage {
    fun store(cookies: List<Cookie>)
    fun retrieve(): List<Cookie>
    fun clear()
}

class ClientCookies(private val storage: CookieStorage = BasicCookieStorage) : Filter {
    override fun invoke(handler: HttpHandler): HttpHandler = { request ->
        val requestWithCookies = request.cookies(storage.retrieve())
        val response = handler(requestWithCookies)
        storage.store(response.cookies())
        response
    }
}

object BasicCookieStorage : CookieStorage {
    val storage = ConcurrentHashMap<String, Cookie>()

    override fun store(cookies: List<Cookie>) = cookies.forEach { storage.put(it.name, it) }

    override fun retrieve(): List<Cookie> = storage.values.toList()

    override fun clear() = storage.clear()
}
