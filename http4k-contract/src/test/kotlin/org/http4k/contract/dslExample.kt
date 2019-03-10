package org.http4k.contract

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.http4k.lens.Path
import org.http4k.lens.Query

val app = contract {
    renderer = OpenApi(ApiInfo("foo", "bar", "boring"), Jackson)
    security = ApiKey(Query.required("the_api_key"), { true })

    routes += "/echo" / Path.of("message") meta {
        summary = "summary of this route"
        description = "some rambling description of what this thing actually does"
        produces += APPLICATION_JSON
        tags += Tag("tag3")
        tags += Tag("tag1")
        returning(FORBIDDEN to "no way jose")
    } bindContract GET to { msg -> { Response(OK).body(msg) } }

    routes += "/simples" bindContract GET to { Response(OK) }
}
