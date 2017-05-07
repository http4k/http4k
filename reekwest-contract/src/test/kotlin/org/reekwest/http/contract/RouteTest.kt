package org.reekwest.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.ContentType.Companion.TEXT_PLAIN
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Request.Companion.post
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.with
import org.reekwest.http.lens.Body
import org.reekwest.http.lens.Header
import org.reekwest.http.lens.LensFailure
import org.reekwest.http.lens.Path
import org.reekwest.http.lens.Query
import org.reekwest.http.lens.missing

class RouteTest {

    @Test
    fun `validates contract - success`() {
        val header = Header.required("header")
        val query = Query.required("query")
        val body = Body.string(TEXT_PLAIN).required()
        val route = Route("").header(header).query(query).body(body).at(GET).bind { _: Request -> Response(OK) }

        assertThat((route.router(Root))(get("").with(header to "value", query to "value", body to "hello")), present())
    }

    @Test
    fun `validates contract - failure`() {
        val header = Header.required("header")
        val query = Query.required("query")
        val body = Body.string(TEXT_PLAIN).required()
        val route = Route("").header(header).query(query).body(body).at(GET).bind { _: Request -> Response(OK) }

        val invalidRequest = get("").with(header to "value", body to "hello")
        assertThat((route.router(Root))(invalidRequest), present())
        assertThat({ (route.router(Root))(invalidRequest)?.invoke(invalidRequest) },
            throws(equalTo(LensFailure(query.meta.missing()))))
    }

    @Test
    fun `0 parts - matches route`() {
        val route = Route("").at(GET).bind({ _: Request -> Response(OK) })
        val router = route.router(Root)
        assertThat(router(get("")), present())
        assertThat(router(post("")), absent())
        assertThat(router(get("/bob")), absent())
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

    private fun checkMatching(route: ServerRoute, valid: String, expected: String) {
        val routerOnNoPrefix = route.router(Root)
        assertThat(routerOnNoPrefix(get("")), absent())
        assertThat(routerOnNoPrefix(post(valid)), absent())
        assertThat(routerOnNoPrefix(get(valid))?.invoke(get(valid))?.bodyString(), equalTo(expected))

        val routerOnPrefix = route.router(Root / "somePrefix")
        assertThat(routerOnPrefix(get("/somePrefix")), absent())
        assertThat(routerOnPrefix(post("/somePrefix/$valid")), absent())
        assertThat(routerOnPrefix(get("/somePrefix/$valid"))?.invoke(get(valid))?.bodyString(), equalTo(expected))
    }
}