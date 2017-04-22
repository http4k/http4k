package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Uri.Companion.uri

class FilterTest {

    private val echoHeaders = { req: Request -> req.headers.fold(Response(OK)) { memo, next -> memo.header(next.first, next.second) } }
    private val addRequestHeader = Filter { next -> { next(it.header("hello", "world")) } }
    private val addResponseHeader = Filter { next -> { next(it).header("goodbye", "cruel") } }

    @Test
    fun `can manipulate value on way in and out of service`() {
        val svc = addRequestHeader.then(addResponseHeader).then(echoHeaders)
        val response = svc(Request(Method.GET, uri("/")))
        assertThat(response.header("hello"), equalTo("world"))
        assertThat(response.header("goodbye"), equalTo("cruel"))
    }
}