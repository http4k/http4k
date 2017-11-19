package org.http4k.contract

import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Argo
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int

class ContractDsl {
    var renderer: ContractRenderer = NoRenderer
    var security: Security = NoSecurity
    var descriptionPath = ""
    var summary: String = "<unknown>"
}
fun contractDsl(fn: ContractDsl.() -> Unit): Any = ContractDsl().apply(fn).run {
    this
}

val asd = contractRoute {} / "asd" / Path.int().of("hey") bindContract GET to { { Response(Status.OK) } }


val b = contractRoute {
    summary = "summary of this route"
    description = "some rambling description of what this thing actually does"
    produces += ContentType.APPLICATION_JSON
    tags += listOf(Tag("tag3"), Tag("tag1"))
    headers += Header.optional("header", "description of the header")
    queries += Query.optional("header", "description of the header")
} //"/echo" / Path.of("message")

val a = contractDsl {
    renderer = OpenApi(ApiInfo("foo", "bbb", "asd"), Argo)
    security = ApiKey(Query.required("the_api_key"), { true })
//        routes += contractRoute"/echo" / Path.of("message")
//    header Header . optional ("header", "description of the header")
//    bindContract Method . GET to { msg -> { Response(Status.OK).body(msg) } }
//    meta meta {
//        summary = "summary of this route"
//        description = "some rambling description of what this thing actually does"
//        produces += ContentType.APPLICATION_JSON
//        tags += Tag("tag3")
//        tags += Tag("tag1")
//        returning("no way jose" to Status.FORBIDDEN)
//    },
//
//    "/simples" bindContract Method.GET to { Response(Status.OK) } meta RouteMeta("a simple endpoint")
//    )
}
