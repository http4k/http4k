package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.NoSecurity.filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.lens.Query
import org.http4k.lens.int
import org.junit.jupiter.api.Test

class ApiKeySecurityTest {

    @Test
    fun `valid API key is granted access and result carried through`() {
        val param = Query.int().required("name")
        val next = HttpHandler { Response(OK).body("hello") }

        val response = ApiKeySecurity(param, { true }).filter(next)(Request(GET, "?name=1"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

    @Test
    fun `OPTIONS request is granted access even with no API key if toggled off`() {
        val param = Query.int().required("name")
        val next = HttpHandler { Response(OK).body("hello") }

        val response = ApiKeySecurity(param, { true }, false).filter(next)(Request(OPTIONS, "/"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

    @Test
    fun `missing API key is unauthorized`() {
        val param = Query.int().required("name")
        val next = HttpHandler { Response(OK).body("hello") }

        val response = ApiKeySecurity(param, { true }).filter(next)(Request(GET, ""))

        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `bad API key is unauthorized`() {
        val param = Query.int().required("name")
        val next = HttpHandler { Response(OK).body("hello") }

        val response = ApiKeySecurity(param, { true }).filter(next)(Request(GET, "?name=asdasd"))

        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `unknown API key is unauthorized`() {
        val param = Query.int().required("name")
        val next = HttpHandler { Response(OK).body("hello") }

        val response = ApiKeySecurity(param, { false }).filter(next)(Request(GET, "?name=1"))

        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `no security is rather lax`() {
        val response = filter { Response(OK).body("hello") }(Request(Method.GET, ""))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

}
