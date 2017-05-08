package org.http4k.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.http4k.http.core.Method.GET
import org.http4k.http.core.Status.Companion.OK
import org.http4k.http.core.Uri.Companion.uri

class FilterTest {

    private val echoHeaders = { req: Request -> req.headers.fold(Response(OK)) { memo, next -> memo.header(next.first, next.second) } }

    private val addRequestHeader = Filter { next -> { next(it.header("hello", "world")) } }
    private val addResponseHeader = Filter { next -> { next(it).header("goodbye", "cruel") } }

    @Test
    fun `can manipulate value on way in and out of service`() {
        val svc = addRequestHeader.then(addResponseHeader).then(echoHeaders)
        val response = svc(Request(GET, uri("/")))
        assertThat(response.header("hello"), equalTo("world"))
        assertThat(response.header("goodbye"), equalTo("cruel"))
    }

    @Test
    fun `applies in order of chain`() {
        val minus10 = Filter { next -> { next(it.replaceHeader("hello", (it.header("hello")!!.toInt() - 10).toString())) } }
        val double = Filter { next -> { next(it.replaceHeader("hello", (it.header("hello")!!.toInt() * 2).toString())) } }

        val final = double.then(minus10).then(echoHeaders)
        val response = final(Request(GET, uri("/")).header("hello", "10"))
        assertThat(response.header("hello"), equalTo("10"))
    }
}