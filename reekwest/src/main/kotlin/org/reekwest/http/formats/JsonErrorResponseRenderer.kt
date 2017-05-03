package org.reekwest.http.formats

import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.BAD_REQUEST
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.lens.Failure

class JsonErrorResponseRenderer<ROOT : NODE, out NODE : Any>(private val json: Json<ROOT, NODE>) {
    fun badRequest(failures: List<Failure>) =
        Response(BAD_REQUEST).body(
            json.compact(
                json.obj("message" to json.string("Missing/invalid parameters"),
                    "params" to json.array(failures.map {
                        json.obj(
                            "name" to json.string(it.meta.name),
                            "type" to json.string(it.meta.location),
                            //        "datatype" to json.string(p.meta.paramType.name),
                            "required" to json.boolean(it.meta.required),
                            "reason" to json.string(it.javaClass.simpleName))
                    }))))

    fun notFound(): Response = Response(NOT_FOUND)
        .body(
            json.compact(
                json.obj("message" to json.string("No route found on this path. Have you used the correct HTTP verb?"))))
}