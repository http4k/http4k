package org.http4k.jsonrpc

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.string
import org.http4k.routing.bind
import org.junit.Test

data class Add(val first: Int, val second: Int)

private object Calculator {
    fun add(it: Add): Int = it.first + it.second

    fun fails(): Void = throw RuntimeException("Boom!")
}

class ManualMappingJsonRpcServiceTest : JsonRpcServiceContract<JsonNode>(Jackson, { json ->

    val addParams = Params<JsonNode, Add> {
        Add(it["first"].asText().toInt(), it["second"].asText().toInt())
    }
    val addResult: Result<Int, JsonNode> = Result { json.number(it) }

    jsonRpc(json) {
        method("add", handler(setOf("first", "second"), addParams, addResult, Calculator::add))
        method("addNoArray", handler(addParams, addResult, Calculator::add))
        method("fails", handler(Result { json.nullNode() }, Calculator::fails))
    }
})

class AutoMappingJsonRpcServiceTest : JsonRpcServiceContract<JsonNode>(Jackson, { json ->

    jsonRpc(auto(json)) {
        method("add", handler(setOf("first", "second"), Calculator::add))
        method("addNoArray", handler(Calculator::add))
        method("fails", handler(Calculator::fails))
    }
})

abstract class JsonRpcServiceContract<ROOT: Any>(json: JsonLibAutoMarshallingJson<ROOT>,
                                                        builder: (JsonLibAutoMarshallingJson<ROOT>) -> JsonRpcService<ROOT, ROOT>) {

    private val rpc = "/rpc" bind builder(json)

    @Test
    fun `rpc call with named parameters returns result`() {
        assertThat(
                rpcRequest("add", "{\"first\": 5, \"second\": 3}", "1"),
                hasSuccessResponse("8", "1")
        )
        assertThat(
                rpcRequest("add", "{\"second\": 4, \"first\": 7}", "2"),
                hasSuccessResponse("11", "2")
        )
    }

    @Test
    fun `rpc call with positional parameters returns result`() {
        assertThat(
                rpcRequest("add", "[5, 3]", "1"),
                hasSuccessResponse("8", "1")
        )
    }

    @Test
    fun `rpc call as notification with named parameters returns no content`() {
        assertThat(
                rpcRequest("add", "{\"first\": 5, \"second\": 3}"),
                hasNoContentResponse()
        )
    }

    @Test
    fun `rpc call as notification with positional parameters returns no content`() {
        assertThat(
                rpcRequest("add", "[5, 3]"),
                hasNoContentResponse()
        )
    }

    @Test
    fun `rpc call with invalid parameters returns error`() {
        assertThat(
                rpcRequest("add", "{\"first\": 5, \"second\": \"a\"}", "1"),
                hasErrorResponse(-32600, "Invalid Request", "1")
        )
    }

    @Test
    fun `rpc call with positional parameters when not supported returns error`() {
        assertThat(
                rpcRequest("addNoArray", "[5, 3]", "1"),
                hasErrorResponse(-32600, "Invalid Request", "1")
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
                rpcRequestWithBody("{\"jsonrpc\": \"2.0\", \"method\": \"foobar, \"params\": \"bar\", \"baz]"),
                hasErrorResponse(-32700, "Parse error", null)
        )
    }

    @Test
    fun `rpc call with invalid request returns error`() {
        assertThat(
                rpcRequestWithBody("5"),
                hasErrorResponse(-32600, "Invalid Request", null)
        )
    }

    @Test
    fun `rpc call with invalid version type returns error`() {
        assertThat(
                rpcRequestWithBody("{\"jsonrpc\": 2.0, \"method\": \"a\", \"id\" : 1}"),
                hasErrorResponse(-32600, "Invalid Request", "1")
        )
    }

    @Test
    fun `rpc call with invalid version value returns error`() {
        assertThat(
                rpcRequestWithBody("{\"jsonrpc\": \"2.1\", \"method\": \"a\", \"id\" : 1}"),
                hasErrorResponse(-32600, "Invalid Request", "1")
        )
    }

    @Test
    fun `rpc call with invalid method type returns error`() {
        assertThat(
                rpcRequestWithBody("{\"jsonrpc\": \"2.0\", \"method\": 2, \"id\" : 1}"),
                hasErrorResponse(-32600, "Invalid Request", "1")
        )
    }

    @Test
    fun `rpc call with invalid params type returns error`() {
        assertThat(
                rpcRequest("add", "3", "1"),
                hasErrorResponse(-32600, "Invalid Request", "1")
        )
    }

    @Test
    fun `rpc call with invalid id type returns error`() {
        assertThat(
                rpcRequest("add", "3", "[1]"),
                hasErrorResponse(-32600, "Invalid Request", null)
        )
    }

    @Test
    fun `rpc call with id type as string returns result`() {
        assertThat(
                rpcRequest("add", "{\"first\": 5, \"second\": 3}", "\"1\""),
                hasSuccessResponse("8", "\"1\"")
        )
    }

    @Test
    fun `rpc call with id type as null returns result`() {
        assertThat(
                rpcRequest("add", "{\"first\": 5, \"second\": 3}", "null"),
                hasSuccessResponse("8", "null")
        )
    }

    @Test
    fun `rpc call as notification with invalid version value returns error`() {
        assertThat(
                rpcRequestWithBody("{\"jsonrpc\": \"2.1\", \"method\": \"a\"}"),
                hasErrorResponse(-32600, "Invalid Request", null)
        )
    }

    @Test
    fun `rpc call to method that throws exception returns error`() {
        assertThat(
                rpcRequest("fails", id = "1"),
                hasErrorResponse(-32000, "Server error", "1")
        )
    }

    @Test
    fun `rpc call using GET http method returns method not allowed`() {
        assertThat(
                rpc(Request(Method.GET, "/rpc")),
                hasStatus(Status.METHOD_NOT_ALLOWED) and hasBody("")
        )
    }

    @Test
    fun `rpc call using wrong content type returns unsupported media type`() {
        assertThat(
                rpc(Request(Method.POST, "/rpc")),
                hasStatus(Status.UNSUPPORTED_MEDIA_TYPE) and hasBody("")
        )
    }

    @Test
    fun `rpc batch call with invalid json returns error`() {
        assertThat(
                rpcRequestWithBody("[{\"jsonrpc\": \"2.0\", \"method\": \"a\", \"id\": 1}, {\"jsonrpc\": \"2.0\", \"method\":"),
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
                        rpcJson("add", "{\"first\": 5, \"second\": 3}", "1"),
                        rpcJson("add", "[5, 3]", "2"),
                        rpcJson("add", "3", "[1]"),
                        rpcJson("add", "{\"first\": 5, \"second\": 3}"),
                        rpcJson("add", "[5, 3]"),
                        "1"
                ).joinToString(",", "[", "]")),
                hasBatchResponse(
                        Success("8", "1"),
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
                        rpcJson("add", "{\"first\": 5, \"second\": 3}"),
                        rpcJson("add", "[5, 3]")
                ).joinToString(",", "[", "]")),
                hasNoContentResponse()
        )
    }

    private fun rpcRequest(method: String, params: String? = null, id: String? = null): Response =
            rpcRequestWithBody(rpcJson(method, params, id))

    private fun rpcJson(method: String, params: String? = null, id: String? = null): String =
            "{\"jsonrpc\": \"2.0\", \"method\": \"$method\"" +
                    params?.let { ", \"params\": $params" }.orEmpty() +
                    id?.let { ", \"id\": $id" }.orEmpty() +
                    "}"

    private fun rpcRequestWithBody(body: String): Response = rpc(Request(Method.POST, "/rpc")
            .with(Body.string(ContentType.APPLICATION_JSON).toLens() of body))

    private fun hasSuccessResponse(result: String, id: String): Matcher<Response> =
            hasResponse(Success(result, id))

    private fun hasNoContentResponse(): Matcher<Response> =
            hasStatus(Status.NO_CONTENT) and
                    hasContentType(ContentType.APPLICATION_JSON) and
                    hasBody("")

    private fun hasErrorResponse(code: Int, message: String, id: String?): Matcher<Response> =
            hasResponse(Error(code, message, id))

    private fun hasResponse(response: ExpectedResponse): Matcher<Response> =
            hasStatus(Status.OK) and
                    hasContentType(ContentType.APPLICATION_JSON) and
                    hasBody(response.toString())

    private fun hasBatchResponse(vararg responses: ExpectedResponse): Matcher<Response> =
            hasStatus(Status.OK) and
                    hasContentType(ContentType.APPLICATION_JSON) and
                    hasBody(responses.joinToString(",", "[", "]"))

    private abstract class ExpectedResponse

    private data class Success(private val result: String, private val id: String): ExpectedResponse() {
        override fun toString() = "{\"jsonrpc\":\"2.0\",\"result\":$result,\"id\":$id}"
    }

    private data class Error(private val code: Int, private val message: String, private val id: String?): ExpectedResponse() {
        override fun toString() =
                "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":$code,\"message\":\"$message\"},\"id\":$id}"
    }
}