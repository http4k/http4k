package org.http4k.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Failure
import org.http4k.lens.Header
import org.http4k.lens.Missing
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.lensFailureWith
import org.http4k.lens.string
import org.junit.jupiter.api.Test

class ContractRouteTest {

    @Test
    fun `validates contract - success`() {
        val headerLens = Header.required("header")
        val queryLens = Query.required("query")
        val bodyLens = Body.string(TEXT_PLAIN).toLens()
        val route = "/" meta {
            headers += headerLens
            queries += queryLens
            body = bodyLens
        } bindContract GET to { _: Request -> Response(OK) }

        assertThat(route.toRouter(Root).match(Request(GET, "").with(headerLens of "value", queryLens of "value", bodyLens of "hello")), present())
    }

    @Test
    fun `validates contract - failure`() {
        val headerLens = Header.required("header")
        val queryLens = Query.required("query")
        val bodyLens = Body.string(TEXT_PLAIN).toLens()
        val route = "/" meta {
            headers += headerLens
            queries += queryLens
            body = bodyLens
        } bindContract GET to { _: Request -> Response(OK) }

        val invalidRequest = Request(GET, "").with(headerLens of "value", bodyLens of "hello")
        val actual = route.toRouter(Root).match(invalidRequest)
        assertThat(actual, present())
        assertThat({ actual?.invoke(invalidRequest) },
            throws(lensFailureWith(Missing(queryLens.meta), overallType = Failure.Type.Missing)))
    }

    @Test
    fun `can build a request from a route`() {
        val path1 = Path.int().of("sue")
        val path2 = Path.string().of("bob")
        val pair = path1 / path2 meta {
            summary = ""
            queries += Query.required("")
        } bindContract GET
        val route = pair to { _, _ -> { _: Request -> Response(OK) } }
        val request = route.newRequest(Uri.of("http://rita.com"))

        request.with(path1 of 123, path2 of "hello world") shouldMatch equalTo(Request(GET, "http://rita.com/123/hello%20world"))
    }

    @Test
    fun `can build a request from a string`() {
        val path1 = Path.int().of("sue")
        val path2 = Path.string().of("bob")
        val pair = "/bob" bindContract GET
        val request = pair.newRequest(Uri.of("http://rita.com"))

        request.with(path1 of 123, path2 of "hello world") shouldMatch equalTo(Request(GET, "http://rita.com/bob"))
    }

    @Test
    fun `can build a request from a routespec`() {
        val path1 = Path.int().of("sue")
        val path2 = Path.string().of("bob")
        val request = (path1 / path2 meta {
            queries += Query.required("")
        } bindContract GET).newRequest(Uri.of("http://rita.com"))

        request.with(path1 of 123, path2 of "hello world") shouldMatch equalTo(Request(GET, "http://rita.com/123/hello%20world"))
    }

    @Test
    fun `0 parts - matches route`() {
        val route = "/" bindContract GET to { Response(OK) }
        val router = route.toRouter(Root)
        assertThat(router.match(Request(GET, "/")), present())
        assertThat(router.match(Request(POST, "/")), absent())
        assertThat(router.match(Request(GET, "/bob")), absent())
    }

    @Test
    fun `1 part - matches route`() {
        fun matched(value: String) = { _: Request -> Response(OK).body(value) }

        checkMatching(Path.of("value") bindContract GET to ::matched, "/value", "value")
    }

    @Test
    fun `2 parts - matches route`() {
        fun matched(value1: String, value2: String) = { _: Request -> Response(OK).body(value1 + value2) }

        checkMatching(Path.of("value") / Path.of("value2") bindContract GET to ::matched, "/value1/value2", "value1value2")
    }

    @Test
    fun `3 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String) = { _: Request -> Response(OK).body(value1 + value2 + value3) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") bindContract GET to ::matched, "/value1/value2/value3", "value1value2value3")
    }

    @Test
    fun `4 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String) = { _: Request -> Response(OK).body(value1 + value2 + value3 + value4) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") bindContract GET to ::matched, "/value1/value2/value3/value4", "value1value2value3value4")
    }

    @Test
    fun `5 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String) =
                { _: Request -> Response(OK).body(value1 + value2 + value3 + value4 + value5) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") / Path.of("value5")
                bindContract GET to ::matched, "/value1/value2/value3/value4/value5", "value1value2value3value4value5")
    }

    @Test
    fun `6 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String, value6: String) =
                { _: Request -> Response(OK).body(value1 + value2 + value3 + value4 + value5 + value6) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") /
                Path.of("value5") / Path.of("value6")
                bindContract GET to ::matched, "/value1/value2/value3/value4/value5/value6",
                "value1value2value3value4value5value6")
    }

    @Test
    fun `7 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String, value6: String,
                    value7: String) = { _: Request -> Response(OK).body(value1 + value2 + value3 + value4 +
                value5 + value6 + value7) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") /
                Path.of("value5") / Path.of("value6") / Path.of("value7")
                bindContract GET to ::matched, "/value1/value2/value3/value4/value5/value6/value7",
                "value1value2value3value4value5value6value7")
    }

    @Test
    fun `8 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String, value6: String,
                    value7: String, value8: String) = { _: Request -> Response(OK).body(value1 + value2 +
                value3 + value4 + value5 + value6 + value7 + value8) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") /
                Path.of("value5") / Path.of("value6") / Path.of("value7") / Path.of("value8")
                bindContract GET to ::matched, "/value1/value2/value3/value4/value5/value6/value7/value8",
                "value1value2value3value4value5value6value7value8")
    }

    @Test
    fun `9 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String, value6: String,
                    value7: String, value8: String, value9: String) = { _: Request -> Response(OK).body(value1 +
                value2 + value3 + value4 + value5 + value6 + value7 + value8 + value9) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") /
                Path.of("value5") / Path.of("value6") / Path.of("value7") / Path.of("value8") /
                Path.of("value9")
                bindContract GET to ::matched, "/value1/value2/value3/value4/value5/value6/value7/value8/value9",
                "value1value2value3value4value5value6value7value8value9")
    }

    @Test
    fun `10 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String, value5: String, value6: String,
                    value7: String, value8: String, value9: String, value10: String) = { _: Request -> Response(OK)
                .body(value1 + value2 + value3 + value4 + value5 + value6 + value7 + value8 + value9 + value10) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") /
                Path.of("value5") / Path.of("value6") / Path.of("value7") / Path.of("value8") /
                Path.of("value9") / Path.of("value10")
                bindContract GET to ::matched, "/value1/value2/value3/value4/value5/value6/value7/value8/value9/value10",
                "value1value2value3value4value5value6value7value8value9value10")
    }

    @Test
    fun `11 parts - unsupported`() {
        assertThat({
        Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4")/
                Path.of("value5") / Path.of("value6") / Path.of("value7") / Path.of("value8") /
                Path.of("value9") / Path.of("value10") / Path.of("value11")
        }, throws<UnsupportedOperationException>())
    }

    private fun checkMatching(route: ContractRoute, valid: String, expected: String) {
        val routerOnNoPrefix = route.toRouter(Root)
        assertThat(routerOnNoPrefix.match(Request(GET, "")), absent())
        assertThat(routerOnNoPrefix.match(Request(POST, valid)), absent())
        assertThat(routerOnNoPrefix.match(Request(GET, valid))?.invoke(Request(GET, valid))?.bodyString(), equalTo(expected))

        val routerOnPrefix = route.toRouter(Root / "somePrefix")
        assertThat(routerOnPrefix.match(Request(GET, "/somePrefix")), absent())
        assertThat(routerOnPrefix.match(Request(POST, "/somePrefix/$valid")), absent())
        assertThat(routerOnPrefix.match(Request(GET, "/somePrefix/$valid"))?.invoke(Request(GET, valid))?.bodyString(), equalTo(expected))
    }
}
