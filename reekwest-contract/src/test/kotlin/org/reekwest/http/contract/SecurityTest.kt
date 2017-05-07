package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.lens.int

class SecurityTest {

    @org.junit.Test
    fun `valid API key is granted access and result carried through`() {
        val param = org.reekwest.http.lens.Query.int().required("name")
        val next: org.reekwest.http.core.HttpHandler = { org.reekwest.http.core.Response(org.reekwest.http.core.Status.Companion.OK).body("hello") }

        val response = org.reekwest.http.contract.ApiKey(param, { true }).filter(next)(org.reekwest.http.core.Request.Companion.get("?name=1"))

        com.natpryce.hamkrest.assertion.assertThat(response.status, com.natpryce.hamkrest.equalTo(org.reekwest.http.core.Status.Companion.OK))
        com.natpryce.hamkrest.assertion.assertThat(response.bodyString(), com.natpryce.hamkrest.equalTo("hello"))
    }

    @org.junit.Test
    fun `missing API key is unauthorized`() {
        val param = org.reekwest.http.lens.Query.int().required("name")
        val next: org.reekwest.http.core.HttpHandler = { org.reekwest.http.core.Response(org.reekwest.http.core.Status.Companion.OK).body("hello") }

        val response = org.reekwest.http.contract.ApiKey(param, { true }).filter(next)(org.reekwest.http.core.Request.Companion.get(""))

        com.natpryce.hamkrest.assertion.assertThat(response.status, com.natpryce.hamkrest.equalTo(org.reekwest.http.core.Status.Companion.UNAUTHORIZED))
    }

    @org.junit.Test
    fun `bad API key is unauthorized`() {
        val param = org.reekwest.http.lens.Query.int().required("name")
        val next: org.reekwest.http.core.HttpHandler = { org.reekwest.http.core.Response(org.reekwest.http.core.Status.Companion.OK).body("hello") }

        val response = org.reekwest.http.contract.ApiKey(param, { true }).filter(next)(org.reekwest.http.core.Request.Companion.get("?name=asdasd"))

        com.natpryce.hamkrest.assertion.assertThat(response.status, com.natpryce.hamkrest.equalTo(org.reekwest.http.core.Status.Companion.UNAUTHORIZED))
    }

    @org.junit.Test
    fun `unknown API key is unauthorized`() {
        val param = org.reekwest.http.lens.Query.int().required("name")
        val next: org.reekwest.http.core.HttpHandler = { org.reekwest.http.core.Response(org.reekwest.http.core.Status.Companion.OK).body("hello") }

        val response = org.reekwest.http.contract.ApiKey(param, { false }).filter(next)(org.reekwest.http.core.Request.Companion.get("?name=1"))

        com.natpryce.hamkrest.assertion.assertThat(response.status, com.natpryce.hamkrest.equalTo(org.reekwest.http.core.Status.Companion.UNAUTHORIZED))
    }

    @org.junit.Test
    fun `no security is rather lax`() {
        val response = (org.reekwest.http.contract.NoSecurity.filter({ Response(OK).body("hello") }))(get(""))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }

}

