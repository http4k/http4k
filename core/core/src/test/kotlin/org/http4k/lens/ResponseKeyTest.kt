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
    fun `value present`() {
        assertThat(ResponseKey.of<String>("hello")(response), equalTo("world"))
        assertThat(ResponseKey.optional<String>("hello")(response), equalTo("world"))
    }

    @Test
    fun `required value missing`() {
        val requiredResponseKey = ResponseKey.of<String>("world")
        assertThat(
            { requiredResponseKey(response) },
            throws(lensFailureWith<Response>(Missing(requiredResponseKey.meta), overallType = Failure.Type.Missing))
        )
    }

    @Test
    fun `optional value missing`() {
        val optionalResponseKey = ResponseKey.optional<String>("world")
        assertThat(optionalResponseKey(Response(OK)), absent())
    }

    @Test
    fun `required value missing - plain response`() {
        val requiredResponseKey = ResponseKey.of<String>("world")
        assertThat(
            { requiredResponseKey(Response(OK)) },
            throws(lensFailureWith<Response>(Missing(requiredResponseKey.meta), overallType = Failure.Type.Missing))
        )
    }

    @Test
    fun `optional value missing - plain response`() {
        val optionalResponseKey = ResponseKey.optional<String>("world")
        assertThat(optionalResponseKey(response), absent())
    }

    @Test
    fun `sets value on response`() {
        val requiredKey = ResponseKey.of<String>("bob")
        assertThat(requiredKey(response.with(requiredKey of "hello")), equalTo("hello"))

        val optionalKey = ResponseKey.optional<String>("bob")
        assertThat(optionalKey(response.with(optionalKey of "hello")), equalTo("hello"))
    }

    @Test
    fun `sets null value on response`() {
        val optionalKey = ResponseKey.optional<String>("hello")
        assertThat(optionalKey(response.with(optionalKey of null)), equalTo(null))
    }

    @Test
    fun `required context value makes it through routing`() {
        val app: HttpHandler =
            routes("" bind GET to { req: Request -> Response(OK).with(ResponseKey.of<String>("foo") of "bar") })
        val resp = Filter { next ->
            {
                next(it).let { it.body(ResponseKey.of<String>("foo")(it)) }
            }
        }.then(app)(Request(GET, ""))

        assertThat(resp.bodyString(), equalTo("bar"))
    }

    @Test
    fun `optional context value makes it through routing`() {
        val app: HttpHandler =
            routes("" bind GET to { req: Request -> Response(OK).with(ResponseKey.optional<String>("foo") of "bar") })
        val resp = Filter { next ->
            {
                next(it).let { it.body(ResponseKey.optional<String>("foo")(it)!!) }
            }
        }.then(app)(Request(GET, ""))

        assertThat(resp.bodyString(), equalTo("bar"))
    }
}
