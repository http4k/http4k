package org.http4k.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.PreFlightExtraction.Companion.IgnoreBody
import org.http4k.contract.PreFlightExtraction.Companion.None
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.contract.simple.SimpleJson
import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
import org.http4k.format.Jackson.auto
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.RoutedResponse
import org.http4k.routing.RoutingHttpHandlerContract
import org.http4k.routing.bind
import org.http4k.routing.matchAndInvoke
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

class ContractRoutingHttpHandlerTest : RoutingHttpHandlerContract() {

    private data class ARandomObject(val field: String)

    private val requiredQuery = Query.int().required("foo")
    private val requiredBody = Body.auto<ARandomObject>().toLens()

    val contractRoutes = listOf(
        validPath bindContract GET to { it -> Response(OK).with(header of header(it)) },
        "/bad-request" bindContract GET to { it -> requiredQuery(it); Response(OK) },
        "/bad-request-query-via-meta" meta { queries += requiredQuery } bindContract GET to { _ -> Response(OK) },
        "/bad-request-body" bindContract GET to { it -> requiredBody(it); Response(OK) },
        "/bad-request-body-via-meta" meta { receiving(requiredBody) } bindContract GET to { _ -> Response(OK) },
        "/bad-request-body-ignore-body" meta {
            receiving(requiredBody)
            preFlightExtraction = IgnoreBody
        } bindContract GET to { _ -> Response(OK) },
        "/bad-request-body-ignore-all" meta {
            queries += Query.required("myHeader")
            receiving(requiredBody)
            preFlightExtraction = None
        } bindContract GET to { _ -> Response(OK) }
    )

    private val header = Header.optional("FILTER")

    override val expectedNotFoundBody =
        "{\"message\":\"No route found on this path. Have you used the correct HTTP verb?\"}"

    override val handler =
        ServerFilters.CatchAll()
            .then(
                contract {
                    renderer = SimpleJson(Jackson)
                    routes += contractRoutes.toList()
                }
            )

    @Test
    fun `can bind under a route match`() {
        val app =
            routes(
                "/hello" bind routes("/there" bind
                    contract {
                        renderer = SimpleJson(Jackson)
                        routes += validPath bindContract GET to { _ -> Response(OK) }
                    })
            )
        assertThat(app(Request(GET, "/hello/there$validPath")), hasStatus(OK))
    }

    @Test
    fun `by default the description lives at the route`() {
        Request(GET, "/root")
        val response = ("/root" bind contract {
            renderer = SimpleJson(Jackson)
            security = ApiKeySecurity(Query.required("goo"), { false })
        }).invoke(Request(GET, "/root"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("""{"resources":{}}"""))
    }

    @Test
    fun `passes through contract filter`() {
        val filter = Filter { next ->
            { next(it.with(header of "true")) }
        }

        val root = "/root" bind contract {
            descriptionPath = "/docs"
            routes += "/" bindContract GET to { it -> Response(OK).with(header of header(it)) }
        }
        val withRoute = filter.then(root)

        val response = withRoute(Request(GET, "/root"))

        assertThat(response.status, equalTo(OK))
        assertThat(header(response), equalTo("true"))
    }

    @Test
    fun `traffic goes to the path specified`() {
        val root = routes(
            "/root/bar" bind contract {
                routes += "/foo/bar" / Path.of("world") bindContract GET to { { Response(OK) } }
            })
        val response = root(Request(GET, "/root/bar/foo/bar/hello")) as RoutedResponse

        assertThat(response.status, equalTo(OK))
        assertThat(response.xUriTemplate, equalTo(UriTemplate.from("/root/bar/foo/bar/{world}")))
    }

    @Test
    fun `OPTIONS traffic goes to the path specified but is intercepted by the default response if the route does NOT response to OPTIONS`() {
        val root = routes(
            "/root/bar" bind contract {
                routes += "/foo/bar" bindContract GET to { _ -> Response(NOT_IMPLEMENTED) }
            }
        )
        val response = root(Request(OPTIONS, "/root/bar/foo/bar"))

        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `OPTIONS traffic ignores missing body`() {
        val root = routes(
            "/root/bar" bind contract {
                routes += "/foo/bar" meta {
                    receiving(requiredBody)
                } bindContract POST to { _ -> Response(NOT_IMPLEMENTED) }
            }
        )

        val response = root(Request(OPTIONS, "/root/bar/foo/bar"))

        assertThat(response.status, equalTo(OK))
    }

    @Test
    override fun `stacked filter application - applies when not found`() {
        val filtered = filterAppending("foo").then(routes(handler))
        val request = Request(GET, "/not-found").header("host", "host")

        assertThat(filtered.matchAndInvoke(request), absent())
        assertThat(filtered(request), hasStatus(NOT_FOUND) and hasHeader("res-header", "foo"))
    }

//    @Test
//    fun `OPTIONS traffic goes to the path and handler specified if the route responds to OPTIONS`() {
//        val root = org.http4k.routing.routes(
//            "/root/bar" bind contract {
//               routes += "/foo/bar" bindContract OPTIONS to { Response(NOT_IMPLEMENTED) })
//        }
//        val response = root(Request(OPTIONS, "/root/bar/foo/bar"))
//
//        assertThat(response.status, equalTo(NOT_IMPLEMENTED))
//    }

    @Test
    fun `identifies called route using identity header on request`() {
        val root = routes(
            "/root" bind contract {
                routes += Path.fixed("hello") / Path.of("world") bindContract GET to { _, _ -> { Response(OK) } }
            }
        )
        val response: RoutedResponse = root(Request(GET, "/root/hello/planet")) as RoutedResponse

        assertThat(response.status, equalTo(OK))
        assertThat(response.xUriTemplate, equalTo(UriTemplate.from("/root/hello/{world}")))
    }

    @Test
    fun `applies security and responds with a 401 to unauthorized requests`() {
        val root = "/root" bind contract {
            security = ApiKeySecurity(Query.required("key"), { it == "bob" })
            routes += "/bob" bindContract GET to { _ -> Response(OK) }
        }
        val response = root(Request(GET, "/root/bob?key=sue"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `security from endpoint overrides global security`() {
        val credentials = Credentials("bill", "password")
        val root = "/root" bind contract {
            security = ApiKeySecurity(Query.required("key"), { it == "valid" })
            routes += "/bob" meta {
            } bindContract GET to { _ -> Response(OK) }
            routes += "/bill" meta {
                security = BasicAuthSecurity("realm", credentials)
            } bindContract GET to { _ -> Response(OK) }
        }

        // non overridden security
        assertThat(root(Request(GET, "/root/bob?key=valid")).status, equalTo(OK))

        // nothing is rejected
        assertThat(root(Request(GET, "/root/bill")).status, equalTo(UNAUTHORIZED))

        // no basic auth is rejected
        assertThat(root(Request(GET, "/root/bill?key=valid")).status, equalTo(UNAUTHORIZED))

        // basic auth is invoked
        assertThat(
            ClientFilters.BasicAuth(credentials)
                .then(root)(Request(GET, "/root/bill?key=invalid")).status, equalTo(OK)
        )
    }

    @Test
    fun `pre-security filter is applied before security`() {
        val root = "/root" bind contract {
            security = ApiKeySecurity(Query.required("key"), { it == "bob" })
            routes += "/bob" bindContract GET to { _ -> Response(OK) }
        }.withFilter { next ->
            {
                next(it.query("key", "bob"))
            }
        }

        assertThat(root(Request(GET, "/root/bob")).status, equalTo(OK))
    }

    @Test
    fun `post-security filter is applied after security`() {
        val root = "/root" bind contract {
            security = ApiKeySecurity(Query.required("key"), { it == "bob" })
            routes += "/bob" bindContract GET to { it -> Response(OK).body(it.body) }
        }.withPostSecurityFilter { next ->
            {
                next(it.body("body"))
            }
        }

        assertThat(root(Request(GET, "/root/bob?key=bob")), hasStatus(OK).and(hasBody("body")))
    }

    @Test
    fun `applies security and responds with a 200 to authorized requests`() {
        val root = "/root" bind contract {
            security = ApiKeySecurity(Query.required("key"), { it == "bob" })
            routes += "/bob" bindContract GET to { _ -> Response(OK) }
        }

        val response = root(Request(GET, "/root/bob?key=bob"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `can change path to description route`() {
        val response = ("/root/foo" bind contract {
            descriptionPath = "/docs/swagger.json"
        }).invoke(Request(GET, "/root/foo/docs/swagger.json"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `applies description security to description route and responds with 401 to unauthorized access`() {
        val handler = "/root" bind contract {
            descriptionPath = "/docs/swagger.json"
            descriptionSecurity = ApiKeySecurity(Query.required("key"), { false })
        }
        val response = handler(Request(GET, "/root/docs/swagger.json"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `applies description security and responds with a 200 to authorized requests`() {
        val handler = "/root" bind contract {
            renderer = SimpleJson(Jackson)
            descriptionSecurity = ApiKeySecurity(Query.required("key"), { it == "valid" })
        }

        val response = handler(Request(GET, "/root?key=valid"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("""{"resources":{}}"""))
    }

    @Test
    fun `only calls filters once`() {
        val filter = Filter { next ->
            {
                next(it.header("foo", "bar"))
            }
        }
        val contract = contract {
            routes += "/test" bindContract GET to
                { it -> Response(OK).body(it.headerValues("foo").toString()) }
        }

        val response = (filter.then(contract))(Request(GET, "/test"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("[bar]"))
    }

    @Test
    fun `handles bad request from handler - parameter`() {
        assertThat(
            handler(Request(GET, "/bad-request")),
            hasStatus(BAD_REQUEST) and
                hasBody("""{"message":"Missing/invalid parameters","params":[{"name":"foo","type":"query","datatype":"integer","required":true,"reason":"Missing"}]}""")
        )
    }

    @Test
    fun `handles bad request from handler - body`() {
        assertThat(
            handler(Request(GET, "/bad-request-body")),
            hasStatus(BAD_REQUEST) and
                hasBody("""{"message":"Missing/invalid parameters","params":[{"name":"body","type":"body","datatype":"object","required":true,"reason":"Invalid"}]}""")
        )
    }

    @Test
    fun `handles bad request via contract violation - parameter`() {
        assertThat(
            handler(Request(GET, "/bad-request-query-via-meta")),
            hasStatus(BAD_REQUEST) and
                hasBody("""{"message":"Missing/invalid parameters","params":[{"name":"foo","type":"query","datatype":"integer","required":true,"reason":"Missing"}]}""")
        )
    }

    @Test
    fun `handles bad request via contract-violation - body`() {
        assertThat(
            handler(Request(GET, "/bad-request-body-via-meta")),
            hasStatus(BAD_REQUEST) and
                hasBody("""{"message":"Missing/invalid parameters","params":[{"name":"body","type":"body","datatype":"object","required":true,"reason":"Invalid"}]}""")
        )
    }

    @Test
    fun `can disable body checking by overriding pre-request-extraction`() {
        assertThat(handler(Request(GET, "/bad-request-body-ignore-body")), hasStatus(OK))
    }

    @Test
    fun `can all paramter checking by overriding pre-request-extraction`() {
        assertThat(handler(Request(GET, "/bad-request-body-ignore-all")), hasStatus(OK))
    }
}
