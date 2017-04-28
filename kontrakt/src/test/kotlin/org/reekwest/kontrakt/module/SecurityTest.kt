package org.reekwest.kontrakt.module

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Status.Companion.UNAUTHORIZED
import org.reekwest.http.core.body
import org.reekwest.kontrakt.Query
import org.reekwest.kontrakt.int

class SecurityTest {

    @Test
    fun `valid API key is granted access and result carried through`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { true }).filter(next)(get("?name=1"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

    @Test
    fun `missing API key is unauthorized`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { true }).filter(next)(get(""))

        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `bad API key is unauthorized`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { true }).filter(next)(get("?name=asdasd"))

        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `unknown API key is unauthorized`() {
        val param = Query.int().required("name")
        val next: HttpHandler = { Response(OK).body("hello") }

        val response = ApiKey(param, { false }).filter(next)(get("?name=1"))

        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `no security is rather lax`() {
        val response = NoSecurity.filter({ Response(OK).body("hello") })(get(""))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

}

