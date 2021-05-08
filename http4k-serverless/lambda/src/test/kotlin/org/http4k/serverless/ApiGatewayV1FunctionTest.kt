package org.http4k.serverless

import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.junit.jupiter.api.Test

// is this needed any more?
class ApiGatewayV1FunctionTest {

    @Test
    fun `can process request`() {
        assertOutput(
            ApiGatewayV1Function { Response(NOT_FOUND).body("helloworld") },
            mapOf(
                "path" to "/path",
                "queryStringParameters" to mapOf("query" to "value"),
                "body" to "input body",
                "headers" to mapOf("c" to "d"),
                "isBase64Encoded" to false,
                "httpMethod" to "GET"
            ),
            mapOf(
                "statusCode" to 404,
                "headers" to emptyMap<String, String>(),
                "body" to "aGVsbG93b3JsZA==",
                "isBase64Encoded" to true
            )
        )
    }
}
