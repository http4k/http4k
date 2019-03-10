package org.http4k.contract

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Jackson.auto
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.RoutedResponse
import org.http4k.routing.RoutingHttpHandlerContract
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

class ContractRoutingHttpHandlerTest : RoutingHttpHandlerContract() {

    private data class ARandomObject(val field: String)

    private val requiredQuery = Query.int().required("foo")
    private val requiredBody = Body.auto<ARandomObject>().toLens()

    override val handler =
        contract(SimpleJson(Jackson), "/",
            validPath bindContract GET to { Response(OK).with(header of header(it)) },
            "/bad-request" bindContract GET to { requiredQuery(it); Response(OK) },
            "/bad-request-query-via-meta" meta { queries += requiredQuery } bindContract GET to { Response(OK) },
            "/bad-request-body" bindContract GET to { requiredBody(it); Response(OK) },
            "/bad-request-body-via-meta" meta { receiving(requiredBody) } bindContract GET to { Response(OK) }
        )

    private val header = Header.optional("FILTER")

    override val expectedNotFoundBody = "{\"message\":\"No route found on this path. Have you used the correct HTTP verb?\"}"

    @Test
    fun `by default the description lives at the route`() {
        Request(GET, "/root")
        val response = ("/root" bind contract(SimpleJson(Jackson), security = ApiKey(Query.required("goo"), { false }))).invoke(Request(GET, "/root"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("""{"resources":{}}"""))
    }

    @Test
    fun `passes through contract filter`() {
        val filter = Filter { next ->
            { next(it.with(header of "true")) }
        }

        val root = "/root" bind contract(SimpleJson(Jackson), "/docs",
            "/" bindContract GET to { Response(OK).with(header of header(it)) })
        val withRoute = filter.then(root)

        val response = withRoute(Request(GET, "/root"))

        assertThat(response.status, equalTo(OK))
        assertThat(header(response), equalTo("true"))
    }

    @Test
    fun `traffic goes to the path specified`() {
        val root = routes(
            "/root/bar" bind contract(
                "/foo/bar" / Path.of("world") bindContract GET to { _ -> { Response(OK) } })
        )
        val response = root(Request(GET, "/root/bar/foo/bar/hello")) as RoutedResponse

        assertThat(response.status, equalTo(OK))
        assertThat(response.xUriTemplate, equalTo(UriTemplate.from("/root/bar/foo/bar/{world}")))
    }

    @Test
    fun `OPTIONS traffic goes to the path specified but is intercepted by the default response if the route does NOT response to OPTIONS`() {
        val root = routes(
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
        val root = routes(
            "/root" bind contract(
                Path.fixed("hello") / Path.of("world") bindContract GET to { _, _ -> { Response(OK) } })
        )
        val response: RoutedResponse = root(Request(GET, "/root/hello/planet")) as RoutedResponse

        assertThat(response.status, equalTo(OK))
        assertThat(response.xUriTemplate, equalTo(UriTemplate.from("/root/hello/{world}")))
    }

    @Test
    fun `applies security and responds with a 401 to unauthorized requests`() {
        val root = "/root" bind contract(SimpleJson(Jackson), "", ApiKey(Query.required("key"), { it == "bob" }),
            "/bob" bindContract GET to { Response(OK) }
        )

        val response = root(Request(GET, "/root/bob?key=sue"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `pre-security filter is applied before security`() {
        val root = "/root" bind contract(SimpleJson(Jackson), "", ApiKey(Query.required("key"), { it == "bob" }),
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
        val root = "/root" bind contract(SimpleJson(Jackson), "", ApiKey(Query.required("key"), { it == "bob" }),
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
        val root = "/root" bind contract(SimpleJson(Jackson), "", ApiKey(Query.required("key"), { it == "bob" }),
            "/bob" bindContract GET to { Response(OK) }
        )

        val response = root(Request(GET, "/root/bob?key=bob"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `can change path to description route`() {
        val response = ("/root/foo" bind contract(SimpleJson(Jackson), "/docs/swagger.json"))
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

        val response = (filter.then(contract))(Request(GET, "/test"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("[bar]"))
    }

    @Test
    fun `handles bad request - parameter`() {
        assertThat(handler(Request(GET, "/bad-request")),
            hasStatus(BAD_REQUEST) and
                hasBody("""{"message":"Missing/invalid parameters","params":[{"name":"foo","type":"query","datatype":"integer","required":true,"reason":"Missing"}]}"""))
    }

    @Test
    fun `handles bad request via contract violation - parameter`() {
        assertThat(handler(Request(GET, "/bad-request-query-via-meta")),
            hasStatus(BAD_REQUEST) and
                hasBody("""{"message":"Missing/invalid parameters","params":[{"name":"foo","type":"query","datatype":"integer","required":true,"reason":"Missing"}]}"""))
    }

    @Test
    fun `handles bad request - body`() {
        assertThat(handler(Request(GET, "/bad-request-body-via-meta")),
            hasStatus(BAD_REQUEST) and
                hasBody("""{"message":"Missing/invalid parameters","params":[{"name":"body","type":"body","datatype":"object","required":true,"reason":"Invalid"}]}"""))
    }
}
