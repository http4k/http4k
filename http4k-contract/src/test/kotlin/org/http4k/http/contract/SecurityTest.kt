package org.http4k.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.Response
import org.http4k.http.core.Status.Companion.OK
import org.http4k.http.lens.int

class SecurityTest {

    @org.junit.Test
    fun `valid API key is granted access and result carried through`() {
        val param = org.http4k.http.lens.Query.int().required("name")
        val next: org.http4k.http.core.HttpHandler = { org.http4k.http.core.Response(org.http4k.http.core.Status.Companion.OK).body("hello") }

        val response = org.http4k.http.contract.ApiKey(param, { true }).filter(next)(org.http4k.http.core.Request.Companion.get("?name=1"))

        com.natpryce.hamkrest.assertion.assertThat(response.status, com.natpryce.hamkrest.equalTo(org.http4k.http.core.Status.Companion.OK))
        com.natpryce.hamkrest.assertion.assertThat(response.bodyString(), com.natpryce.hamkrest.equalTo("hello"))
    }

    @org.junit.Test
    fun `missing API key is unauthorized`() {
        val param = org.http4k.http.lens.Query.int().required("name")
        val next: org.http4k.http.core.HttpHandler = { org.http4k.http.core.Response(org.http4k.http.core.Status.Companion.OK).body("hello") }

        val response = org.http4k.http.contract.ApiKey(param, { true }).filter(next)(org.http4k.http.core.Request.Companion.get(""))

        com.natpryce.hamkrest.assertion.assertThat(response.status, com.natpryce.hamkrest.equalTo(org.http4k.http.core.Status.Companion.UNAUTHORIZED))
    }

    @org.junit.Test
    fun `bad API key is unauthorized`() {
        val param = org.http4k.http.lens.Query.int().required("name")
        val next: org.http4k.http.core.HttpHandler = { org.http4k.http.core.Response(org.http4k.http.core.Status.Companion.OK).body("hello") }

        val response = org.http4k.http.contract.ApiKey(param, { true }).filter(next)(org.http4k.http.core.Request.Companion.get("?name=asdasd"))

        com.natpryce.hamkrest.assertion.assertThat(response.status, com.natpryce.hamkrest.equalTo(org.http4k.http.core.Status.Companion.UNAUTHORIZED))
    }

    @org.junit.Test
    fun `unknown API key is unauthorized`() {
        val param = org.http4k.http.lens.Query.int().required("name")
        val next: org.http4k.http.core.HttpHandler = { org.http4k.http.core.Response(org.http4k.http.core.Status.Companion.OK).body("hello") }

        val response = org.http4k.http.contract.ApiKey(param, { false }).filter(next)(org.http4k.http.core.Request.Companion.get("?name=1"))

        com.natpryce.hamkrest.assertion.assertThat(response.status, com.natpryce.hamkrest.equalTo(org.http4k.http.core.Status.Companion.UNAUTHORIZED))
    }

    @org.junit.Test
    fun `no security is rather lax`() {
        val response = (org.http4k.http.contract.NoSecurity.filter({ Response(OK).body("hello") }))(get(""))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

}

