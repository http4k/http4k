package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.RouterMatch
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.Unmatched
import org.junit.jupiter.api.Test

class ContractRouteTest {

    @Test
    fun `can build a request from a route`() {
        val path1 = Path.int().of("sue")
        val path2 = Path.string().of("bob")
        val pair = path1 / path2 meta {
            summary = ""
            queries += Query.required("")
        } bindContract GET
        val route = pair to { _, _ -> HttpHandler{ Response(OK) } }
        val request = route.newRequest(Uri.of("http://rita.com"))

        assertThat(request.with(path1 of 123, path2 of "hello world"), equalTo(Request(GET, "http://rita.com/123/hello%20world")))
    }

    @Test
    fun `can build a request from a string`() {
        val path1 = Path.int().of("sue")
        val path2 = Path.string().of("bob")
        val pair = "/bob" bindContract GET
        val request = pair.newRequest(Uri.of("http://rita.com"))

        assertThat(request.with(path1 of 123, path2 of "hello world"), equalTo(Request(GET, "http://rita.com/bob")))
    }

    @Test
    fun `can build a request from a routespec`() {
        val path1 = Path.int().of("sue")
        val path2 = Path.string().of("bob")
        val request = (path1 / path2 meta {
            queries += Query.required("")
        } bindContract GET).newRequest(Uri.of("http://rita.com/base"))

        assertThat(request.with(path1 of 123, path2 of "hello world"), equalTo(Request(GET, "http://rita.com/123/hello%20world")))
    }

    @Test
    fun `can build a request from a routespec - no base`() {
        val path1 = Path.int().of("sue")
        val path2 = Path.string().of("bob")
        val request = (path1 / path2 meta {
            queries += Query.required("")
        } bindContract GET).newRequest()

        assertThat(request.with(path1 of 123, path2 of "hello world"), equalTo(Request(GET, "/123/hello%20world")))
    }

    @Test
    fun `0 parts - matches route`() {
        val handler = HttpHandler { Response(OK) }
        val route = "/" bindContract GET to handler
        val router = route.toRouter(Root)
        assertThat(router.match(Request(GET, "/")), equalTo(MatchingHandler(handler, router.description) as RouterMatch))
        assertThat(router.match(Request(POST, "/")), equalTo(Unmatched(router.description) as RouterMatch))
        assertThat(router.match(Request(GET, "/bob")), equalTo(Unmatched(router.description) as RouterMatch))
    }

    @Test
    fun `new requests`() {
        fun assertRequest(contractRoute: ContractRoute, expected: String) {
            assertThat(contractRoute.newRequest(Uri.of("http://foo.com")), equalTo(Request(GET, expected)))
        }

        val handler = HttpHandler { Response(OK) }

        assertRequest("/" bindContract GET to handler, "http://foo.com")
        assertRequest(Path.of("value") bindContract GET to { handler }, "http://foo.com/{value}")
        assertRequest(Path.of("value") / Path.of("value2") bindContract GET to { _, _ -> handler }, "http://foo.com/{value}/{value2}")
        assertRequest(Path.of("value") / Path.of("value2") / Path.of("value3") bindContract GET to { _, _, _ -> handler }, "http://foo.com/{value}/{value2}/{value3}")
        assertRequest(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") bindContract GET to { _, _, _, _ -> handler }, "http://foo.com/{value}/{value2}/{value3}/{value4}")
        assertRequest(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") / Path.of("value5") bindContract GET to { _, _, _, _, _ -> handler }, "http://foo.com/{value}/{value2}/{value3}/{value4}/{value5}")
        assertRequest(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") / Path.of("value5") / Path.of("value6") bindContract GET to { _, _, _, _, _, _ -> handler }, "http://foo.com/{value}/{value2}/{value3}/{value4}/{value5}/{value6}")
        assertRequest(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") / Path.of("value5") / Path.of("value6") / Path.of("value7") bindContract GET to { _, _, _, _, _, _, _ -> handler }, "http://foo.com/{value}/{value2}/{value3}/{value4}/{value5}/{value6}/{value7}")
    }

    @Test
    fun `route as HttpHandler matches as expected`() {
        val route = Path.int().of("value") meta {} bindContract GET to { HttpHandler{ Response(OK) } }

        assertThat(route(Request(GET, "/1")), hasStatus(OK))
        assertThat(route(Request(DELETE, "/1")), hasStatus(NOT_FOUND))
        assertThat(route(Request(GET, "/notInt")), hasStatus(NOT_FOUND))
    }

    @Test
    fun `route as HttpHandler validates security of route`() {
        val route = Path.int().of("value") meta {
            security = ApiKeySecurity(Query.required("foo"), { true })
        } bindContract GET to { HttpHandler{ Response(OK) } }
        assertThat(route(Request(GET, "/1")), hasStatus(UNAUTHORIZED))
        assertThat(route(Request(GET, "/1").query("foo", "bar")), hasStatus(OK))
    }

    @Test
    fun `route as HttpHandler performs pre-extraction of route`() {
        val route = Path.int().of("value") meta {
            queries += Query.required("foo")
        } bindContract GET to { HttpHandler{ Response(OK) } }
        assertThat(route(Request(GET, "/1")), hasStatus(BAD_REQUEST))
        assertThat(route(Request(GET, "/1").query("foo", "bar")), hasStatus(OK))
    }

    @Test
    fun `1 part - matches route`() {
        fun matched(value: String) = HttpHandler { Response(OK).body(value) }

        checkMatching(Path.of("value") bindContract GET to ::matched, "/value", "value")
    }

    @Test
    fun `2 parts - matches route`() {
        fun matched(value1: String, value2: String) = HttpHandler { Response(OK).body(value1 + value2) }

        checkMatching(Path.of("value") / "value2" meta {} bindContract GET to ::matched, "/value1/value2", "value1value2")
    }

    @Test
    fun `3 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String) = HttpHandler { Response(OK).body(value1 + value2 + value3) }

        checkMatching(Path.of("value") / Path.of("value2") / "value3" meta {} bindContract GET to ::matched, "/value1/value2/value3", "value1value2value3")
    }

    @Test
    fun `4 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String) = HttpHandler { Response(OK).body(value1 + value2 + value3 + value4) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / "value4" meta {}
            bindContract GET to ::matched, "/value1/value2/value3/value4", "value1value2value3value4")
    }

    @Test
    fun `5 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String) =
            HttpHandler { Response(OK).body(value1 + value2 + value3 + value4 + value5) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") / "value5" meta {}
            bindContract GET to ::matched, "/value1/value2/value3/value4/value5", "value1value2value3value4value5")
    }

    @Test
    fun `6 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String, value6: String) =
            HttpHandler { Response(OK).body(value1 + value2 + value3 + value4 + value5 + value6) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") /
            Path.of("value5") / "value6" meta {}
            bindContract GET to ::matched, "/value1/value2/value3/value4/value5/value6",
            "value1value2value3value4value5value6")
    }

    @Test
    fun `7 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String, value6: String,
                    value7: String) = HttpHandler {
            Response(OK).body(value1 + value2 + value3 + value4 +
                value5 + value6 + value7)
        }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") /
            Path.of("value5") / Path.of("value6") / "value7" meta {}
            bindContract GET to ::matched, "/value1/value2/value3/value4/value5/value6/value7",
            "value1value2value3value4value5value6value7")
    }

    @Test
    fun `8 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String, value6: String,
                    value7: String, value8: String) = HttpHandler {
            Response(OK).body(value1 + value2 +
                value3 + value4 + value5 + value6 + value7 + value8)
        }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") /
            Path.of("value5") / Path.of("value6") / Path.of("value7") / "value8" meta {}
            bindContract GET to ::matched, "/value1/value2/value3/value4/value5/value6/value7/value8",
            "value1value2value3value4value5value6value7value8")
    }

    @Test
    fun `9 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String, value6: String,
                    value7: String, value8: String, value9: String) = HttpHandler {
            Response(OK).body(value1 +
                value2 + value3 + value4 + value5 + value6 + value7 + value8 + value9)
        }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") /
            Path.of("value5") / Path.of("value6") / Path.of("value7") / Path.of("value8") /
            "value9" meta {}
            bindContract GET to ::matched, "/value1/value2/value3/value4/value5/value6/value7/value8/value9",
            "value1value2value3value4value5value6value7value8value9")
    }

    @Test
    fun `10 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String, value6: String,
                    value7: String, value8: String, value9: String, value10: String) = HttpHandler {
            Response(OK)
                .body(value1 + value2 + value3 + value4 + value5 + value6 + value7 + value8 + value9 + value10)
        }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") /
            Path.of("value5") / Path.of("value6") / Path.of("value7") / Path.of("value8") /
            Path.of("value9") / "value10" meta {}
            bindContract GET to ::matched, "/value1/value2/value3/value4/value5/value6/value7/value8/value9/value10",
            "value1value2value3value4value5value6value7value8value9value10")
    }

    @Test
    fun `11 parts - unsupported`() {
        assertThat({
            Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") /
                Path.of("value5") / Path.of("value6") / Path.of("value7") / Path.of("value8") /
                Path.of("value9") / Path.of("value10") / "value11"
        }, throws<UnsupportedOperationException>())
    }

    private fun checkMatching(route: ContractRoute, valid: String, expected: String) {
        assertThat(route(Request(GET, valid)).bodyString(), equalTo(expected))
        assertThat(route(Request(DELETE, valid)), hasStatus(NOT_FOUND))

        val routerOnNoPrefix = route.toRouter(Root)
        assertThat(routerOnNoPrefix.match(Request(GET, "")), equalTo(Unmatched(routerOnNoPrefix.description) as RouterMatch))
        assertThat(routerOnNoPrefix.match(Request(POST, valid)), equalTo(Unmatched(routerOnNoPrefix.description) as RouterMatch))
        assertThat(routerOnNoPrefix.match(Request(GET, valid)).matchOrNull()?.invoke(Request(GET, valid))?.bodyString(), equalTo(expected))

        val routerOnPrefix = route.toRouter(Root / "somePrefix")
        assertThat(routerOnPrefix.match(Request(GET, "/somePrefix")), equalTo(Unmatched(routerOnPrefix.description) as RouterMatch))
        assertThat(routerOnPrefix.match(Request(POST, "/somePrefix/$valid")), equalTo(Unmatched(routerOnPrefix.description) as RouterMatch))
        assertThat(routerOnPrefix.match(Request(GET, "/somePrefix/$valid")).matchOrNull()?.invoke(Request(GET, valid))?.bodyString(), equalTo(expected))
    }

    private fun RouterMatch.matchOrNull(): HttpHandler? = when (this) {
        is MatchingHandler -> this
        else -> null
    }
}
