package org.http4k.format

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.with
import org.http4k.lens.Failure
import org.http4k.lens.Header.CONTENT_TYPE

class JsonErrorResponseRenderer<out NODE>(private val json: Json<NODE>) {
    fun badRequest(failures: List<Failure>) =
        Response(BAD_REQUEST)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body(
                json {
                    compact(
                        obj("message" to string("Missing/invalid parameters"),
                            "params" to array(failures.map {
                                obj(
                                    "name" to string(it.meta.name),
                                    "type" to string(it.meta.location),
                                    "datatype" to string(it.meta.paramMeta.value),
                                    "required" to boolean(it.meta.required),
                                    "reason" to string(it.javaClass.simpleName))
                            })))
                })

    fun notFound(): Response = Response(NOT_FOUND)
        .body(json {
            compact(
                obj("message" to string("No route found on this path. Have you used the correct HTTP verb?")))
        })
}