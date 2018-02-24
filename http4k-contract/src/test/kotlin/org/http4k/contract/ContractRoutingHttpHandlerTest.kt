package org.http4k.contract

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Argo
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header
import org.http4k.lens.Header.X_URI_TEMPLATE
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.Test

class ContractRoutingHttpHandlerTest {

    private val header = Header.optional("FILTER")

    @Test
    fun `by default the description lives at the route`() {
        Request(GET, "/root")
        val response = ("/root" bind contract(SimpleJson(Argo))).invoke(Request(GET, "/root"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("""{"resources":{}}"""))
    }

    @Test
    fun `passes through contract filter`() {
        val filter = Filter { next ->
            { next(it.with(header of "true")) }
        }

        val root = "/root" bind contract(SimpleJson(Argo), "/docs",
            "/" bindContract GET to { Response(OK).with(header of header(it)) })
        val withRoute = filter.then(root)

        val response = withRoute(Request(GET, "/root"))

        assertThat(response.status, equalTo(OK))
        assertThat(header(response), equalTo("true"))
    }

    @Test
    fun `traffic goes to the path specified`() {
        val root = org.http4k.routing.routes(
            "/root/bar" bind contract(
                "/foo/bar" / Path.of("world") bindContract GET to { _ -> { Response(OK).with(X_URI_TEMPLATE of X_URI_TEMPLATE(it)) } })
        )
        val response = root(Request(GET, "/root/bar/foo/bar/hello"))

        assertThat(response.status, equalTo(OK))
        assertThat(X_URI_TEMPLATE(response), equalTo("/root/bar/foo/bar/{world}"))
    }

    @Test
    fun `OPTIONS traffic goes to the path specified but is intercepted by the default response if the route does NOT response to OPTIONS`() {
        val root = org.http4k.routing.routes(
            "/root/bar" bind contract(
                "/foo/bar" bindContract GET to { Response(NOT_IMPLEMENTED) })
        )
        val response = root(Request(OPTIONS, "/root/bar/foo/bar"))

        assertThat(response.status, equalTo(OK))
    }

//    @Test
//    fun `OPTIONS traffic goes to the path and handler specified if the route responds to OPTIONS`() {
//        val root = org.http4k.routing.routes(
//            "/root/bar" bind contract(
//                "/foo/bar" bindContract OPTIONS to { Response(NOT_IMPLEMENTED) })
//        )
//        val response = root(Request(OPTIONS, "/root/bar/foo/bar"))
//
//        assertThat(response.status, equalTo(NOT_IMPLEMENTED))
//    }

    @Test
    fun `identifies called route using identity header on request`() {
        val root = org.http4k.routing.routes(
            "/root" bind contract(
                Path.fixed("hello") / Path.of("world") bindContract GET to { _, _ -> { Response(OK).with(X_URI_TEMPLATE of X_URI_TEMPLATE(it)) } })
        )
        val response = root(Request(GET, "/root/hello/planet"))

        assertThat(response.status, equalTo(OK))
        assertThat(X_URI_TEMPLATE(response), equalTo("/root/hello/{world}"))
    }

    @Test
    fun `applies security and responds with a 401 to unauthorized requests`() {
        val root = "/root" bind contract(SimpleJson(Argo), "", ApiKey(Query.required("key"), { it == "bob" }),
            "/bob" bindContract GET to { Response(OK) }
        )

        val response = root(Request(GET, "/root/bob?key=sue"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `pre-security filter is applied before security`() {
        val root = "/root" bind contract(SimpleJson(Argo), "", ApiKey(Query.required("key"), { it == "bob" }),
            "/bob" bindContract GET to { Response(OK) }
        ).withFilter(Filter { next ->
            {
                next(it.query("key", "bob"))
            }
        })

        assertThat(root(Request(GET, "/root/bob")).status, equalTo(OK))
    }

    @Test
    fun `post-security filter is applied after security`() {
        val root = "/root" bind contract(SimpleJson(Argo), "", ApiKey(Query.required("key"), { it == "bob" }),
            "/bob" bindContract GET to { Response(OK).body(it.body) }
        ).withPostSecurityFilter(Filter { next ->
            {
                next(it.body("body"))
            }
        })

        assertThat(root(Request(GET, "/root/bob?key=bob")), hasStatus(OK).and(hasBody("body")))
    }

    @Test
    fun `applies security and responds with a 200 to authorized requests`() {
        val root = "/root" bind contract(SimpleJson(Argo), "", ApiKey(Query.required("key"), { it == "bob" }),
            "/bob" bindContract GET to { Response(OK) }
        )

        val response = root(Request(GET, "/root/bob?key=bob"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `can change path to description route`() {
        val response = ("/root/foo" bind contract(SimpleJson(Argo), "/docs/swagger.json"))
            .invoke(Request(GET, "/root/foo/docs/swagger.json"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `only calls filters once`() {
        val filter = Filter { next ->
            {
                next(it.header("foo", "bar"))
            }
        }
        val contract = contract(
            "/test"
                bindContract GET to
                { Response(OK).body(it.headerValues("foo").toString()) })
        val withFilter = filter.then(contract)
        val response = withFilter(Request(GET, "/test"))
        assertThat(response.status, equalTo(OK))
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
        val contract = contract(
            "/test" bindContract GET to {
                assertThat(calledHandler, equalTo(false))
                calledHandler = true
                Response(OK)
            })

        val request = Request(Method.PUT, "/test")
        (filter.then("/" bind contract))(request)
        (filter.then(contract))(request)
        (org.http4k.routing.routes(filter.then(contract)))(request)
        (filter.then(routes(contract)))(request)
    }

}
