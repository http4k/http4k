package org.http4k.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method
import org.http4k.core.Method.GET
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

class RouteTest {

    @Test
    fun `validates contract - success`() {
        val header = Header.required("header")
        val query = Query.required("query")
        val body = Body.string(TEXT_PLAIN).toLens()
        val route = Route("").header(header).query(query).body(body).at(GET).bind { _: Request -> Response(OK) }

        assertThat((route.router(Root))(Request(Method.GET, "").with(header of "value", query of "value", body of "hello")), present())
    }

    @Test
    fun `validates contract - failure`() {
        val header = Header.required("header")
        val query = Query.required("query")
        val body = Body.string(TEXT_PLAIN).toLens()
        val route = Route("").header(header).query(query).body(body).at(GET).bind { _: Request -> Response(OK) }

        val invalidRequest = Request(Method.GET, "").with(header of "value", body of "hello")
        assertThat((route.router(Root))(invalidRequest), present())
        assertThat({ (route.router(Root))(invalidRequest)?.invoke(invalidRequest) },
            throws(lensFailureWith(query.meta.missing())))
    }

    @Test
    fun `can build a request from a route`() {
        val path1 = Path.int().of("sue")
        val path2 = Path.string().of("bob")
        val route = Route("").at(GET) / path1 / path2
        val request = route.newRequest(Uri.of("http://rita.com"))

        request.with(path1 of 123, path2 of "hello world") shouldMatch equalTo(
            Request(GET, "http://rita.com/123/hello+world")
        )
    }

    @Test
    fun `0 parts - matches route`() {
        val route = Route("").at(GET).bind({ _: Request -> Response(OK) })
        val router = route.router(Root)
        assertThat(router(Request(Method.GET, "")), present())
        assertThat(router(Request(Method.POST, "")), absent())
        assertThat(router(Request(Method.GET, "/bob")), absent())
    }

    @Test
    fun `1 part - matches route`() {
        fun matched(value: String) = { _: Request -> Response(OK).body(value) }

        checkMatching(Route("").at(GET) / Path.of("value") bind (::matched), "/value", "value")
    }

    @Test
    fun `2 parts - matches route`() {
        fun matched(value1: String, value2: String) = { _: Request -> Response(OK).body(value1 + value2) }

        checkMatching(Route("").at(GET) / Path.of("value") / Path.of("value2") bind (::matched), "/value1/value2", "value1value2")
    }

    @Test
    fun `3 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String) = { _: Request -> Response(OK).body(value1 + value2 + value3) }

        checkMatching(Route("").at(GET) / Path.of("value") / Path.of("value2") / Path.of("value3") bind (::matched), "/value1/value2/value3", "value1value2value3")
    }

    @Test
    fun `4 parts - matches route`() {
        fun matched(value1: String, value2: String, value3: String, value4: String) = { _: Request -> Response(OK).body(value1 + value2 + value3 + value4) }

        checkMatching(Route("").at(GET) / Path.of("value") / Path.of("value2") / Path.of("value3") / Path.of("value4") bind (::matched), "/value1/value2/value3/value4", "value1value2value3value4")
    }

    @Test (expected = UnsupportedOperationException::class)
    fun `5 parts - unsupported`() {
        Route("").at(GET) / Path.of("value") / Path.of("value")/ Path.of("value") / Path.of("value")/ Path.of("value")
    }

    private fun checkMatching(route: ServerRoute, valid: String, expected: String) {
        val routerOnNoPrefix = route.router(Root)
        assertThat(routerOnNoPrefix(Request(Method.GET, "")), absent())
        assertThat(routerOnNoPrefix(Request(Method.POST, valid)), absent())
        assertThat(routerOnNoPrefix(Request(Method.GET, valid))?.invoke(Request(Method.GET, valid))?.bodyString(), equalTo(expected))

        val routerOnPrefix = route.router(Root / "somePrefix")
        assertThat(routerOnPrefix(Request(Method.GET, "/somePrefix")), absent())
        assertThat(routerOnPrefix(Request(Method.POST, "/somePrefix/$valid")), absent())
        assertThat(routerOnPrefix(Request(Method.GET, "/somePrefix/$valid"))?.invoke(Request(Method.GET, valid))?.bodyString(), equalTo(expected))
    }
}