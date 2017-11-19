package org.http4k.contract

import org.http4k.format.Argo
import org.http4k.lens.Query

class ContractDsl {
    var renderer: ContractRenderer = NoRenderer
    var security: Security = NoSecurity
    var descriptionPath = ""
    var summary: String = "<unknown>"
}
fun contractDsl(fn: ContractDsl.() -> Unit): Any = ContractDsl().apply(fn).run {
    this
}

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
