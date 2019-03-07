package guide.modules.contracts

// for this example we're using Jackson - note that the auto method imported is an extension
// function that is defined on the Jackson instance

import org.http4k.contract.ApiInfo
import org.http4k.contract.ContractRenderer
import org.http4k.contract.OpenApi
import org.http4k.contract.ResponseMeta
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Jackson.auto

data class Foo(val a: String)

val body = Body.auto<List<Foo>>().toLens()

val spec = "/allParameters" meta {
    summary = "Fetches all the information about parameters."
    description = "Returns all values"
    returning(
        ResponseMeta(
            "Returns parameter info list.",
            Response(OK).with(Body.auto<List<Foo>>().toLens() of listOf(Foo("bob"))),
            "parameter-list"
        )
    )
} bindContract GET to { Response(OK) }

// by default, the OpenAPI docs live at the root of the contract context, but we can override it..
fun main() {
    val openApi: ContractRenderer = OpenApi(ApiInfo("", ""), Jackson)
    val handler = contract(openApi, spec)
    println(handler(Request(GET, "/")))
}


