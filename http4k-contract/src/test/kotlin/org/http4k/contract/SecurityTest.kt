package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Query
import org.http4k.lens.int
import org.junit.jupiter.api.Test

class SecurityTest {

    @Test
    fun `valid API key is granted access and result carried through`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { true }).filter(next)(Request(Method.GET, "?name=1"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

    @Test
    fun `OPTIONS request is granted access even with no API key if toggled off`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { true }, false).filter(next)(Request(Method.OPTIONS, "/"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

    @Test
    fun `missing API key is unauthorized`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { true }).filter(next)(Request(Method.GET, ""))

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
    }

    @Test
    fun `bad API key is unauthorized`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { true }).filter(next)(Request(Method.GET, "?name=asdasd"))

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
    }

    @Test
    fun `unknown API key is unauthorized`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { false }).filter(next)(Request(Method.GET, "?name=1"))

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
    }

    @Test
    fun `no security is rather lax`() {
        val response = (NoSecurity.filter({ Response(OK).body("hello") }))(Request(Method.GET, ""))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

}

