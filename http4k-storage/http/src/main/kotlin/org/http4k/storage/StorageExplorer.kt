package org.http4k.storage

import org.http4k.contract.contract
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.OpenAPIJackson
import org.http4k.contract.openapi.OpenAPIJackson.auto
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.NoSecurity
import org.http4k.contract.security.Security
import org.http4k.contract.ui.swaggerUi
import org.http4k.core.Body
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
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters.Cors
import org.http4k.format.AutoMarshalling
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun <reified T : Any> Storage<T>.asHttpHandler(storageSecurity: Security = NoSecurity,
                                                      autoMarshalling: AutoMarshalling = OpenAPIJackson): HttpHandler {
    val bodyLens = Body.auto<T>().toLens()

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
            swaggerUi(Uri.of("/api/openapi"), "Storage Explorer (${T::class.java.simpleName})")
        )
    )
}

inline fun <reified T : Any> get(bodyLens: BiDiBodyLens<T>, storage: Storage<T>) =
    "storage" / Path.of("key") meta {
        summary = "Get an object by key"
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
        summary = "Delete an object by key"
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
        summary = "Store an object by key"
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
    summary = "List all stored object keys"
    returning(OK)
} bindContract GET to { req: Request ->
    Response(OK).body(storage.keySet(keyPrefix(req)).sorted().joinToString("\n"))
}

inline fun <reified T : Any> deletePrefix(storage: Storage<T>) = "storage" meta {
    queries += keyPrefix
    summary = "Delete all objects with the key prefix"
    returning(ACCEPTED)
    returning(NOT_FOUND)
} bindContract DELETE to { req: Request ->
    Response(if (storage.removeAll(keyPrefix(req))) ACCEPTED else NOT_FOUND)
}
