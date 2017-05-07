package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Filter
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Status.Companion.UNAUTHORIZED
import org.reekwest.http.core.with
import org.reekwest.http.formats.Argo
import org.reekwest.http.lens.Header
import org.reekwest.http.lens.Path
import org.reekwest.http.lens.Query

class RouteModuleTest {

    private val header = Header.optional("FILTER")
    private val routeModule = RouteModule(Root, SimpleJson(Argo), Filter {
        next -> { next(it.with(header to "true")) }
    })

    @Test
    fun `by default the description lives at the route`() {
        val response = routeModule.toHttpHandler()(get(""))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("""{"resources":{}}"""))
    }

    @Test
    fun `passes through module filter`() {
        val response = routeModule.withRoute(Route("").at(GET) bind {
                Response(OK).with(header to header(it))
        }).toHttpHandler()(get(""))

        assertThat(response.status, equalTo(OK))
        assertThat(header(response), equalTo("true"))
    }

    @Test
    fun `identifies called route using identity header on request`() {

        val response = routeModule.withRoute(Route("").at(GET) / Path.fixed("hello") / Path.of("world") bind {
            _, _ ->
            {
                Response(OK).with(X_REEKWEST_ROUTE_IDENTITY to X_REEKWEST_ROUTE_IDENTITY(it))
            }
        }).toHttpHandler()(get("/hello/planet"))

        assertThat(response.status, equalTo(OK))
        assertThat(X_REEKWEST_ROUTE_IDENTITY(response), equalTo("/hello/{world}"))
    }

    @Test
    fun `applies security and responds with a 401 to unauthorized requests`() {
        val response = routeModule
            .securedBy(ApiKey(Query.required("key"), { it == "bob" }))
            .toHttpHandler()(get("?key=sue"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `applies security and responds with a 200 to authorized requests`() {
        val response = routeModule
            .securedBy(ApiKey(Query.required("key"), { it == "bob" }))
            .toHttpHandler()(get("?key=bob"))
        assertThat(response.status, equalTo(OK))
    }

}