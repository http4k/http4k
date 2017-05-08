package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.http.core.HttpHandler
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.Response
import org.http4k.http.core.Status
import org.http4k.http.core.Status.Companion.OK
import org.http4k.http.lens.Query
import org.http4k.http.lens.int

class SecurityTest {

    @org.junit.Test
    fun `valid API key is granted access and result carried through`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { true }).filter(next)(get("?name=1"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

    @org.junit.Test
    fun `missing API key is unauthorized`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { true }).filter(next)(get(""))

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
    }

    @org.junit.Test
    fun `bad API key is unauthorized`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { true }).filter(next)(get("?name=asdasd"))

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
    }

    @org.junit.Test
    fun `unknown API key is unauthorized`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { false }).filter(next)(get("?name=1"))

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
    }

    @org.junit.Test
    fun `no security is rather lax`() {
        val response = (NoSecurity.filter({ Response(OK).body("hello") }))(get(""))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

}

