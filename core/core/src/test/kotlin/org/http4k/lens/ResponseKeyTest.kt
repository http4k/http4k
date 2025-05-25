package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.routing.ResponseWithContext
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

class ResponseKeyTest {

    private val response = ResponseWithContext(Response(OK), mapOf("hello" to "world"))

    @Test
    fun `value present`() = runBlocking {
        assertThat(ResponseKey.of<String>("hello")(response), equalTo("world"))
    }

    @Test
    fun `value missing`() = runBlocking {
        val requiredResponseKey = ResponseKey.of<String>("world")
        assertThat(
            { requiredResponseKey(response) },
            throws(lensFailureWith<Response>(Missing(requiredResponseKey.meta), overallType = Failure.Type.Missing))
        )
    }

    @Test
    fun `sets value on response`() = runBlocking {
        val key = ResponseKey.of<String>("bob")
        val withResponseKey = response.with(key of "hello")
        assertThat(key(withResponseKey), equalTo("hello"))
    }

    @Test
    fun `context value makes it through routing`() = runBlocking {
        val app: HttpHandler =
            routes("" bind GET to { req: Request -> Response(OK).with(ResponseKey.of<String>("foo") of "bar") })
        val resp = Filter { next ->
            {
                next(it).let { it.body(ResponseKey.of<String>("foo")(it)) }
            }
        }.then(app)(Request(GET, ""))

        assertThat(resp.bodyString(), equalTo("bar"))
    }
}
