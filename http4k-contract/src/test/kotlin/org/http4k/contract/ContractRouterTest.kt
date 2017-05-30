package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.format.Argo
import org.http4k.lens.Header
import org.http4k.lens.Header.X_URI_TEMPLATE
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.junit.Test

class ContractRouterTest {

    private val header = Header.optional("FILTER")
    private val contractRouter = ContractRouter(Root, SimpleJson(Argo), Filter {
        next -> { next(it.with(header of "true")) }
    })

    @Test
    fun `by default the description lives at the route`() {
        val response = contractRouter.toHttpHandler()(Request(Method.GET, ""))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("""{"resources":{}}"""))
    }

    @Test
    fun `passes through contract filter`() {
        val response = contractRouter.withRoute(Route("").at(GET) bind {
            Response(OK).with(header of header(it))
        }).toHttpHandler()(Request(Method.GET, ""))

        assertThat(response.status, equalTo(OK))
        assertThat(header(response), equalTo("true"))
    }

    @Test
    fun `identifies called route using identity header on request`() {

        val response = contractRouter.withRoute(Route("").at(GET) / Path.fixed("hello") / Path.of("world") bind {
            _, _ ->
            {
                Response(OK).with(X_URI_TEMPLATE of X_URI_TEMPLATE(it))
            }
        }).toHttpHandler()(Request(Method.GET, "/hello/planet"))

        assertThat(response.status, equalTo(OK))
        assertThat(X_URI_TEMPLATE(response), equalTo("/hello/{world}"))
    }

    @Test
    fun `applies security and responds with a 401 to unauthorized requests`() {
        val response = contractRouter
            .securedBy(ApiKey(Query.required("key"), { it == "bob" }))
            .withRoute(Route().at(GET) / "bob" bind { Response(OK)})
            .toHttpHandler()(Request(Method.GET, "/bob?key=sue"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `applies security and responds with a 200 to authorized requests`() {
        val response = contractRouter
            .securedBy(ApiKey(Query.required("key"), { it == "bob" }))
            .withRoute(Route().at(GET) / "bob" bind { Response(OK)})
            .toHttpHandler()(Request(Method.GET, "/bob?key=bob"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `can change path to description route`() {
        val response = contractRouter
            .withDescriptionPath { it / "docs" / "swagger.json" }
            .toHttpHandler()(Request(Method.GET, "/docs/swagger.json"))
        assertThat(response.status, equalTo(OK))
    }

}