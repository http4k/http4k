package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.routing.RequestWithContext
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

class RequestKeyTest {

    private val request = RequestWithContext(Request(GET, ""), mapOf("hello" to "world"))

    @Test
    fun `value present`() {
        assertThat(RequestKey.of<String>("hello")(request), equalTo("world"))
    }

    @Test
    fun `value missing`() {
        val requiredRequestKey = RequestKey.of<String>("world")
        assertThat(
            { requiredRequestKey(request) },
            throws(lensFailureWith<Request>(Missing(requiredRequestKey.meta), overallType = Failure.Type.Missing))
        )
    }

    @Test
    fun `sets value on request`() {
        val key = RequestKey.of<String>("bob")
        val withRequestKey = request.with(key of "hello")
        assertThat(key(withRequestKey), equalTo("hello"))
    }

    @Test
    fun `context value makes it through routing`() {
        val app: HttpHandler =
            routes("" bind GET to { req: Request -> Response(Status.OK).body(RequestKey.of<String>("foo")(req)) })
        val resp = Filter { next ->
            {
                next(it.with(RequestKey.of<String>("foo") of "bar"))
            }
        }.then(app)(Request(GET, ""))
        assertThat(resp.bodyString(), equalTo("bar"))
    }
}
