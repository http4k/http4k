package org.http4k.lens

import com.natpryce.hamkrest.absent
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
    fun `value present`() = runBlocking {
        assertThat(RequestKey.required<String>("hello")(request), equalTo("world"))
        assertThat(RequestKey.optional<String>("hello")(request), equalTo("world"))
    }

    @Test
    fun `required value missing`() = runBlocking {
        val requiredRequestKey = RequestKey.required<String>("world")
        assertThat(
            { requiredRequestKey(request) },
            throws(lensFailureWith<Request>(Missing(requiredRequestKey.meta), overallType = Failure.Type.Missing))
        )
    }

    @Test
    fun `optional value missing`() = runBlocking {
        val optionalRequestKey = RequestKey.optional<String>("world")
        assertThat(optionalRequestKey(request), absent())
    }

    @Test
    fun `sets value on request`() = runBlocking {
        val requiredKey = RequestKey.required<String>("bob")
        assertThat(requiredKey(request.with(requiredKey of "hello")), equalTo("hello"))

        val optionalKey = RequestKey.required<String>("bob")
        assertThat(optionalKey(request.with(optionalKey of "hello")), equalTo("hello"))
    }

    @Test
    fun `required context value makes it through routing`() = runBlocking {
        val app: HttpHandler =
            routes("" bind GET to { req: Request -> Response(Status.OK).body(RequestKey.required<String>("foo")(req)) })
        val resp = Filter { next ->
            {
                next(it.with(RequestKey.required<String>("foo") of "bar"))
            }
        }.then(app)(Request(GET, ""))
        assertThat(resp.bodyString(), equalTo("bar"))
    }

    @Test
    fun `optional context value makes it through routing`() = runBlocking {
        val app: HttpHandler =
            routes("" bind GET to { req: Request -> Response(Status.OK).body(RequestKey.optional<String>("foo")(req)!!) })
        val resp = Filter { next ->
            {
                next(it.with(RequestKey.optional<String>("foo") of "bar"))
            }
        }.then(app)(Request(GET, ""))
        assertThat(resp.bodyString(), equalTo("bar"))
    }
}
