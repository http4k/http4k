package org.http4k.jsonrpc

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNSUPPORTED_MEDIA_TYPE
import org.http4k.core.with
import org.http4k.format.Json
import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.string
import org.http4k.routing.bind
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class Counter {
    private val value: AtomicInteger = AtomicInteger()

    fun increment(amount: Increment): Int = when {
        amount.value == 10 -> throw RuntimeException("Boom!")
        amount.value < 0 -> throw NegativeIncrementException()
        else -> value.addAndGet(amount.value)
    }

    fun currentValue(): Int = value.get()

    data class Increment(val value: Int)

    class NegativeIncrementException : RuntimeException("negative increment not allowed")
}

object CounterErrorHandler : ErrorHandler {
    override fun invoke(error: Throwable): ErrorMessage? = when (error) {
        is Counter.NegativeIncrementException -> NegativeIncrementExceptionMessage()
        else -> null
    }

    private class NegativeIncrementExceptionMessage : ErrorMessage(1, "Increment by negative") {
        override fun <NODE> data(json: Json<NODE>): NODE? =
            json.string("cannot increment counter by negative")
    }
}

abstract class JsonRpcServiceContract<NODE : Any>(builder: (Counter) -> JsonRpcService<NODE>) {

    private val counter = Counter()
    private val rpc = "/rpc" bind builder(counter)

    @Test
    fun `rpc call with named parameters returns result`() {
        assertThat(
            rpcRequest("increment", """{"value": 2}""", "1"),
            hasSuccessResponse("2", "1")
        )
    }

    @Test
    fun `rpc call with positional parameters returns result`() {
        assertThat(
            rpcRequest("increment", "[3]", "1"),
            hasSuccessResponse("3", "1")
        )
    }

    @Test
    fun `rpc call as notification with named parameters returns no content`() {
        assertThat(
            rpcRequest("increment", """{"value": 5}"""),
            hasNoContentResponse()
        )
        assertThat(counter.currentValue(), equalTo(5))
    }

    @Test
    fun `rpc call as notification with positional parameters returns no content`() {
        assertThat(
            rpcRequest("increment", "[4]"),
            hasNoContentResponse()
        )
        assertThat(counter.currentValue(), equalTo(4))
    }

    @Test
    fun `rpc call with no parameters returns result`() {
        rpcRequest("increment", """{"value": 2}""")
        assertThat(
            rpcRequest("current", id = "1"),
            hasSuccessResponse("2", "1")
        )
    }

    @Test
    fun `rpc call with invalid parameters returns error`() {
        assertThat(
            rpcRequest("increment", """{"value": "a"}""", "1"),
            hasErrorResponse(-32602, "Invalid params", "1")
        )
    }

    @Test
    fun `rpc call of non-existent method returns error`() {
        assertThat(
            rpcRequest("other", id = "1"),
            hasErrorResponse(-32601, "Method not found", "1")
        )
    }

    @Test
    fun `rpc call with invalid json returns error`() {
        assertThat(
            rpcRequestWithBody("""{"jsonrpc": "2.0", "method": "foobar, "params": "bar", "baz]"""),
            hasErrorResponse(-32700, "Parse error", null)
        )
    }

    @Test
    fun `rpc call with invalid request returns error`() {
        assertThat(
            rpcRequestWithBody("""{"value": 5}"""),
            hasErrorResponse(-32600, "Invalid Request", null)
        )
    }

    @Test
    fun `rpc call with invalid version type returns error`() {
        assertThat(
            rpcRequestWithBody("""{"jsonrpc": 2.0, "method": "a", "id" : 1}"""),
            hasErrorResponse(-32600, "Invalid Request", "1")
        )
    }

    @Test
    fun `rpc call with invalid version value returns error`() {
        assertThat(
            rpcRequestWithBody("""{"jsonrpc": "2.1", "method": "increment", "params": [4], "id" : 1}"""),
            hasErrorResponse(-32600, "Invalid Request", "1")
        )
    }

    @Test
    fun `rpc call with invalid method type returns error`() {
        assertThat(
            rpcRequestWithBody("""{"jsonrpc": "2.0", "method": 2, "id" : 1}"""),
            hasErrorResponse(-32600, "Invalid Request", "1")
        )
    }

    @Test
    fun `rpc call with invalid params type returns error`() {
        assertThat(
            rpcRequest("increment", "3", "1"),
            hasErrorResponse(-32600, "Invalid Request", "1")
        )
    }

    @Test
    fun `rpc call with invalid id type returns error`() {
        assertThat(
            rpcRequest("increment", "3", "[1]"),
            hasErrorResponse(-32600, "Invalid Request", null)
        )
    }

    @Test
    fun `rpc call with id type as string returns result`() {
        assertThat(
            rpcRequest("increment", """{"value": 5}""", """"1""""),
            hasSuccessResponse("5", """"1"""")
        )
    }

    @Test
    fun `rpc call with id type as null returns result`() {
        assertThat(
            rpcRequest("increment", """{"value": 3}""", "null"),
            hasSuccessResponse("3", "null")
        )
    }

    @Test
    fun `rpc call as notification with invalid version value returns error`() {
        assertThat(
            rpcRequestWithBody("""{"jsonrpc": "2.1", "method": "a"}"""),
            hasErrorResponse(-32600, "Invalid Request", null)
        )
    }

    @Test
    fun `rpc call to method that throws unhandled exception returns error`() {
        assertThat(
            rpcRequest("increment", """{"value": 10}""", "1"),
            hasErrorResponse(-32603, "Internal error", "1")
        )
    }

    @Test
    fun `rpc call using GET http method returns method not allowed`() {
        assertThat(
            rpc(Request(GET, "/rpc")),
            hasStatus(METHOD_NOT_ALLOWED) and hasBody("")
        )
    }

    @Test
    fun `rpc call using wrong content type returns unsupported media type`() {
        assertThat(
            rpc(Request(POST, "/rpc")),
            hasStatus(UNSUPPORTED_MEDIA_TYPE) and hasBody("")
        )
    }

    @Test
    fun `rpc batch call with invalid json returns error`() {
        assertThat(
            rpcRequestWithBody("""[{"jsonrpc": "2.0", "method": "a", "id": 1}, {"jsonrpc": "2.0", "method":"""),
            hasErrorResponse(-32700, "Parse error", null)
        )
    }

    @Test
    fun `rpc batch call with empty array returns error`() {
        assertThat(
            rpcRequestWithBody("[]"),
            hasErrorResponse(-32600, "Invalid Request", null)
        )
    }

    @Test
    fun `rpc batch call with invalid requests returns result`() {
        assertThat(
            rpcRequestWithBody("[1,2,3]"),
            hasBatchResponse(
                Error(-32600, "Invalid Request", null),
                Error(-32600, "Invalid Request", null),
                Error(-32600, "Invalid Request", null)
            )
        )
    }

    @Test
    fun `rpc batch call with mixed invalid and valid normal and notifications returns result`() {
        assertThat(
            rpcRequestWithBody(listOf(
                rpcJson("increment", """{"value": 5}""", "1"),
                rpcJson("increment", "[3]", "2"),
                rpcJson("increment", "3", "[1]"),
                rpcJson("increment", """{"value": 2}"""),
                rpcJson("increment", "[2]"),
                "1"
            ).joinToString(",", "[", "]")),
            hasBatchResponse(
                Success("5", "1"),
                Success("8", "2"),
                Error(-32600, "Invalid Request", null),
                Error(-32600, "Invalid Request", null)
            )
        )
    }

    @Test
    fun `rpc batch call with only notifications returns no content`() {
        assertThat(
            rpcRequestWithBody(listOf(
                rpcJson("increment", """{"value": 5}"""),
                rpcJson("increment", "[3]")
            ).joinToString(",", "[", "]")),
            hasNoContentResponse()
        )
    }

    @Test
    fun `rpc call that throws user exception returns failure`() {
        assertThat(
            rpcRequest("increment", """{"value": -1}""", "1"),
            hasErrorResponse(1, "Increment by negative",
                """"cannot increment counter by negative"""", "1")
        )
    }

    protected fun rpcRequest(method: String, params: String? = null, id: String? = null): Response =
        rpcRequestWithBody(rpcJson(method, params, id))

    private fun rpcJson(method: String, params: String? = null, id: String? = null): String =
        """{"jsonrpc": "2.0", "method": "$method"""" +
            params?.let { """, "params": $params""" }.orEmpty() +
            id?.let { """, "id": $id""" }.orEmpty() +
            "}"

    private fun rpcRequestWithBody(body: String): Response =
        rpc(Request(POST, "/rpc")
            .with(Body.string(APPLICATION_JSON).toLens() of body))

    protected fun hasSuccessResponse(result: String, id: String): Matcher<Response> =
        hasResponse(Success(result, id))

    private fun hasNoContentResponse(): Matcher<Response> =
        hasStatus(NO_CONTENT) and
            hasContentType(APPLICATION_JSON) and
            hasBody("")

    protected fun hasErrorResponse(code: Int, message: String, id: String?): Matcher<Response> =
        hasResponse(Error(code, message, id))

    private fun hasErrorResponse(code: Int, message: String, data: String?, id: String?): Matcher<Response> =
        hasResponse(Error(code, message, data, id))

    private fun hasResponse(response: ExpectedResponse): Matcher<Response> =
        hasStatus(OK) and
            hasContentType(APPLICATION_JSON) and
            hasBody(response.toString())

    private fun hasBatchResponse(vararg responses: ExpectedResponse): Matcher<Response> =
        hasStatus(OK) and
            hasContentType(APPLICATION_JSON) and
            hasBody(responses.joinToString(",", "[", "]"))

    private abstract class ExpectedResponse

    private data class Success(private val result: String, private val id: String) : ExpectedResponse() {
        override fun toString() = """{"jsonrpc":"2.0","result":$result,"id":$id}"""
    }

    private data class Error(private val code: Int, private val message: String,
                             private val data: String?, private val id: String?) : ExpectedResponse() {
        constructor(code: Int, message: String, id: String?) : this(code, message, null, id)

        override fun toString() =
            """{"jsonrpc":"2.0","error":{"code":$code,"message":"$message"""" +
                (data?.let { ""","data":$it""" } ?: "") +
                """},"id":$id}"""
    }
}

abstract class ManualMappingJsonRpcServiceContract<NODE : Any>(json: Json<NODE>) : JsonRpcServiceContract<NODE>({ counter ->
    val incrementParams = Mapping<NODE, Counter.Increment> { Counter.Increment(json.textValueOf(it, "value")!!.toInt()) }
    val intResult: Mapping<Int, NODE> = Mapping { json.number(it) }

    JsonRpc.manual(json, CounterErrorHandler) {
        method("increment", handler(setOf("value"), incrementParams, intResult, counter::increment))
        method("incrementNoArray", handler(incrementParams, intResult, counter::increment))
        method("current", handler(intResult, counter::currentValue))
    }
}) {
    @Test
    fun `rpc call with positional parameters when fields not defined returns error`() {
        assertThat(
            rpcRequest("incrementNoArray", "[3]", "1"),
            hasErrorResponse(-32602, "Invalid params", "1")
        )
    }
}

abstract class AutoMappingJsonRpcServiceContract<NODE : Any>(json: JsonLibAutoMarshallingJson<NODE>) : JsonRpcServiceContract<NODE>({ counter ->
    JsonRpc.auto(json, CounterErrorHandler) {
        method("increment", handler(counter::increment))
        method("incrementDefinedFields", handler(setOf("value", "ignored"), counter::increment))
        method("current", handler(counter::currentValue))
    }
}) {
    @Test
    fun `rpc call with positional parameters when fields defined returns result`() {
        assertThat(
            rpcRequest("incrementDefinedFields", "[3]", "1"),
            hasSuccessResponse("3", "1")
        )
    }
}
