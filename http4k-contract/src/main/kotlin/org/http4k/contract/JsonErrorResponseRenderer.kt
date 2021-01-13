package org.http4k.contract

import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Json
import org.http4k.lens.Header
import org.http4k.lens.LensFailure

class JsonErrorResponseRenderer<NODE>(private val json: Json<NODE>) : ErrorResponseRenderer {
    override fun badRequest(lensFailure: LensFailure) =
        Response(Status.BAD_REQUEST)
            .with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
            .body(
                json {
                    compact(
                        obj("message" to string("Missing/invalid parameters"),
                            "params" to array(lensFailure.failures.map {
                                obj(
                                    "name" to string(it.meta.name),
                                    "type" to string(it.meta.location),
                                    "datatype" to string(it.meta.paramMeta.description),
                                    "required" to boolean(it.meta.required),
                                    "reason" to string(it.javaClass.simpleName))
                            })))
                })

    override fun notFound(): Response = Response(Status.NOT_FOUND)
        .body(json {
            compact(
                obj("message" to string("No route found on this path. Have you used the correct HTTP verb?")))
        })
}
