package org.http4k.storage

import org.http4k.contract.contract
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.OpenAPIJackson
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.NoSecurity
import org.http4k.contract.security.Security
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters.Cors
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static

inline fun <reified T : Any> Storage<T>.asHttpHandler(storageSecurity: Security = NoSecurity): HttpHandler {
    val bodyLens = OpenAPIJackson.autoBody<T>().toLens()
    val static = static(Classpath("/www"))

    return Cors(UnsafeGlobalPermissive).then(
        routes(
            "/api" bind contract {
                renderer = OpenApi3(ApiInfo("Storage Explorer (${T::class.java.simpleName})", "1.0"))
                descriptionPath = "/openapi"
                security = storageSecurity
                routes += listOf(
                    set(bodyLens, this@asHttpHandler),
                    get(bodyLens, this@asHttpHandler),
                    deletePrefix(this@asHttpHandler),
                    list(this@asHttpHandler),
                    delete(this@asHttpHandler)
                )
            },
            "/" bind GET to static
        )
    )
}

inline fun <reified T : Any> get(bodyLens: BiDiBodyLens<T>, storage: Storage<T>) =
    "storage" / Path.of("key") meta {
        returning(OK, NOT_FOUND)
    } bindContract GET to { key ->
        {
            storage[key]
                ?.let { Response(OK).with(bodyLens of it) }
                ?: Response(NOT_FOUND)
        }
    }

inline fun <reified T : Any> delete(storage: Storage<T>) =
    "storage" / Path.of("key") meta {
        returning(ACCEPTED, NOT_FOUND)
    } bindContract DELETE to { key ->
        {
            storage[key]
                ?.let { storage.remove(key); Response(ACCEPTED) }
                ?: Response(NOT_FOUND)
        }
    }

inline fun <reified T : Any> set(bodyLens: BiDiBodyLens<T>, storage: Storage<T>) =
    "storage" / Path.of("key") meta {
        receiving(bodyLens)
        returning(CREATED, ACCEPTED)
    } bindContract POST to { key ->
        {
            val entity = bodyLens(it)
            if (storage[key] != null) {
                storage[key] = entity
                Response(ACCEPTED)
            } else {
                storage[key] = entity
                Response(CREATED)
            }
        }
    }

val keyPrefix = Query.defaulted("keyPrefix", "")

inline fun <reified T : Any> list(storage: Storage<T>) = "storage" meta {
    queries += keyPrefix
    returning(OK)
} bindContract GET to { req: Request ->
    Response(OK).body(storage.keySet(keyPrefix(req)).sorted().joinToString("\n"))
}

inline fun <reified T : Any> deletePrefix(storage: Storage<T>) = "storage" meta {
    queries += keyPrefix
    returning(ACCEPTED)
    returning(NOT_FOUND)
} bindContract DELETE to { req: Request ->
    Response(if (storage.removeAll(keyPrefix(req))) ACCEPTED else NOT_FOUND)
}
