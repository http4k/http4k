package org.reekwest.http.contract.module

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.Test
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Request.Companion.post
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.lens.Path

class RouteTest {

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
        val router = route.router(Root)
        assertThat(router(get("")), absent())
        assertThat(router(post(valid)), absent())
        assertThat(router(get(valid))?.invoke(get(valid))?.bodyString(), equalTo(expected))
    }
}