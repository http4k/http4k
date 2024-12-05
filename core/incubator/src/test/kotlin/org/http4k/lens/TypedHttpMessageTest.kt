package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Moshi.auto
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class MyValue private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<MyValue>(::MyValue)
}

data class MyType(val value: String)

class TypedHttpMessageTest {

    data class MyRequest(val request: Request) : TypedRequest(request) {
        val path by required(Path.value(MyValue))
        var query by required(Query.int())
        var header by optional(Header)
        var defaulted by defaulted(Query.uuid()) { UUID(0, 0) }
    }

    class MyResponse(response: Response) : TypedResponse(response) {
        var requiredHeader by required(Header.int())
        var optionalHeader by optional(Header.uuid())
        var aBody by body(Body.auto<MyType>())
        var `content-type` by optional(Header)
        var myValue by optional(Header.value(MyValue))
    }

    @Test
    fun `required fields`() {
        val out = routes("/{path}" bind GET to { input: Request ->
            val req = MyRequest(input)
            assertThat(req.path, equalTo(MyValue.of(999)))

            assertThat(req.query, equalTo(123))
            req.query = 456
            assertThat(req.query, equalTo(456))

            assertThat(req.query, equalTo(456))

            Response(OK)
        })(Request(GET, "/999").query("query", "123"))

        assertThat(out, hasStatus(OK))

        val resp = MyResponse(Response(OK))
        assertThrows<LensFailure> { resp.requiredHeader }
        resp.requiredHeader = 456
        assertThat(resp.requiredHeader, equalTo(456))

        resp.myValue = MyValue.of(123123123)
        assertThat(resp.myValue, equalTo(MyValue.of(123123123)))
    }

    @Test
    fun `optional fields`() {
        val req = MyRequest(Request(GET, "").query("query", "123"))
        assertThat(req.header, absent())
        req.header = "123456"
        assertThat(req.header, equalTo("123456"))

        val resp = MyResponse(Response(OK))
        assertThat(resp.optionalHeader, absent())
        resp.optionalHeader = UUID(1, 1)
        assertThat(resp.optionalHeader, equalTo(UUID(1, 1)))
    }

    @Test
    fun `defaulted fields`() {
        val req = MyRequest(Request(GET, ""))
        assertThat(req.defaulted, equalTo(UUID(0, 0)))
    }

    @Test
    fun `body can be read and written`() {
        val resp = MyResponse(Response(OK))
        resp.aBody = MyType("hello")
        assertThat(resp.aBody, equalTo(MyType("hello")))
        assertThat(resp.`content-type`, equalTo(APPLICATION_JSON.toHeaderValue()))
    }
}
