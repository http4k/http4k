package org.http4k.routing

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.contract.Root
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.lensFailureWith
import org.http4k.lens.missing
import org.http4k.lens.string
import org.junit.Test

class ServerRouteTest {

    @Test
    fun `validates contract - success`() {
        val header = Header.required("header")
        val query = Query.required("query")
        val body = Body.string(TEXT_PLAIN).toLens()
        val route = "/" % header % query % body to GET bind { _: Request -> Response(OK) } with RouteMeta("")

        assertThat(route.toRouter(Root).match(Request(GET, "").with(header of "value", query of "value", body of "hello")), present())
    }

    @Test
    fun `validates contract - failure`() {
        val header = Header.required("header")
        val query = Query.required("query")
        val body = Body.string(TEXT_PLAIN).toLens()
        val route = "/" % header % query % body to GET bind { _: Request -> Response(OK) } with RouteMeta("")

        val invalidRequest = Request(GET, "").with(header of "value", body of "hello")
        val actual = route.toRouter(Root).match(invalidRequest)
        assertThat(actual, present())
        assertThat({ actual?.invoke(invalidRequest) },
            throws(lensFailureWith(query.meta.missing())))
    }

    @Test
    fun `can build a request from a route`() {
        val path1 = Path.int().of("sue")
        val path2 = Path.string().of("bob")
        val pair = path1 / path2 % Query.required("") to GET
        pair.newRequest(Uri.of("http://rita.com"))
        val route = pair bind { _, _ -> { _: Request -> Response(OK) } } with RouteMeta("")
        val request = route.newRequest(Uri.of("http://rita.com"))

        request.with(path1 of 123, path2 of "hello world") shouldMatch equalTo(Request(GET, "http://rita.com/123/hello+world"))
    }

    @Test
    fun `can build a request from a routespec`() {
        val path1 = Path.int().of("sue")
        val path2 = Path.string().of("bob")
        val request = (path1 / path2 % Query.required("") to GET).newRequest(Uri.of("http://rita.com"))

        request.with(path1 of 123, path2 of "hello world") shouldMatch equalTo(Request(GET, "http://rita.com/123/hello+world"))
    }

    @Test
    fun `0 parts - matches route`() {
        val route = "/" to GET bind { Response(OK) }
        val router = route.toRouter(Root)
        assertThat(router.match(Request(GET, "/")), present())
        assertThat(router.match(Request(POST, "/")), absent())
        assertThat(router.match(Request(GET, "/bob")), absent())
    }

    @Test
    fun `1 part - matches route`() {
        fun matched(value: String) = { _: Request -> Response(OK).body(value) }

        checkMatching(Path.of("value") to GET bind ::matched, "/value", "value")
    }

    @Test
    fun `2 parts - matches route`() {
        fun matched(value1: String, value2: String) = { _: Request -> Response(OK).body(value1 + value2) }

        checkMatching(Path.of("value") / Path.of("value2") to GET bind ::matched, "/value1/value2", "value1value2")
    }

    @Test
    fun `3 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String) = { _: Request -> Response(OK).body(value1 + value2 + value3) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") to GET bind ::matched, "/value1/value2/value3", "value1value2value3")
    }

    @Test
    fun `4 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String) = { _: Request -> Response(OK).body(value1 + value2 + value3 + value4) }

        checkMatching(Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") to GET bind ::matched, "/value1/value2/value3/value4", "value1value2value3value4")
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `5 parts - unsupported`() {
        Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") / Path.of("value5")
    }

    private fun checkMatching(route: ServerRoute, valid: String, expected: String) {
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
