package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Argo
import org.http4k.lens.Header
import org.http4k.lens.Header.X_URI_TEMPLATE
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.routing.by
import org.http4k.routing.contract
import org.http4k.routing.routes
import org.junit.Test

class ContractRoutingHttpHandlerTest {

    private val header = Header.optional("FILTER")

    @Test
    fun `by default the description lives at the route`() {
        Request(GET, "/root")
        val response = ("/root" by contract(SimpleJson(Argo)))(Request(GET, "/root"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("""{"resources":{}}"""))
    }

    @Test
    fun `passes through contract filter`() {
        val filter = Filter {
            next ->
            { next(it.with(header of "true")) }
        }

        val withRoute = filter.then(("/root" by contract(SimpleJson(Argo))).withRoute(Route("").at(GET) bind {
            Response(OK).with(header of header(it))
        }))

        val response = withRoute.invoke(Request(GET, "/root"))

        assertThat(response.status, equalTo(OK))
        assertThat(header(response), equalTo("true"))
    }

    @Test
    fun `identifies called route using identity header on request`() {
        val response = ("/root" by contract(SimpleJson(Argo))).withRoute(Route("").at(GET) / Path.fixed("hello") / Path.of("world") bind {
            _, _ ->
            {
                Response(OK).with(X_URI_TEMPLATE of X_URI_TEMPLATE(it))
            }
        }).invoke(Request(GET, "/root/hello/planet"))

        assertThat(response.status, equalTo(OK))
        assertThat(X_URI_TEMPLATE(response), equalTo("/root/hello/{world}"))
    }

    @Test
    fun `applies security and responds with a 401 to unauthorized requests`() {
        val response = ("/root" by contract(SimpleJson(Argo), "", ApiKey(Query.required("key"), { it == "bob" })))
            .withRoute(Route().at(GET) / "bob" bind { Response(OK) })
            .invoke(Request(GET, "/root/bob?key=sue"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `applies security and responds with a 200 to authorized requests`() {
        val response = ("/root" by contract(SimpleJson(Argo), "", ApiKey(Query.required("key"), { it == "bob" })))
            .withRoute(Route().at(GET) / "bob" bind { Response(OK) })
            .invoke(Request(GET, "/root/bob?key=bob"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `can change path to description route`() {
        val response = ("/root" by contract(SimpleJson(Argo), "/docs/swagger.json"))
            .invoke(Request(GET, "/root/docs/swagger.json"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `only calls filters once`() {
        val filter = Filter {
            next ->
            {
                next(it.header("foo", "bar"))
            }
        }
        val contract = filter.then(contract().withRoute(Route().at(GET) / "test" bind { Response(OK).body(it.headerValues("foo").toString()) }))
        val response = contract(Request(GET, "/test"))
        assertThat(response.bodyString(), equalTo("[bar]"))
    }

    @Test
    fun `only calls filters once - in various combos`() {
        var called = false
        val filter = Filter {
            { req: Request ->
                assertThat(called, equalTo(false))
                called = true
                it(req)
            }
        }

        var calledHandler = false
        val contract = contract().withRoute(Route().at(GET) / "test" bind {
            assertThat(calledHandler, equalTo(false))
            calledHandler = true
            Response(OK) })

        val request = Request(OPTIONS, "/test")
        (filter.then("/" by contract))(request)
        (filter.then(contract))(request)
        routes(filter.then(contract))(request)
        (filter.then(routes(contract)))(request)
    }

}