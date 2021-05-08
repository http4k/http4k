package org.http4k.serverless

import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.junit.jupiter.api.Test

// is this needed any more?
class ApiGatewayV2FunctionTest {

    @Test
    fun `can process request`() {
        assertOutput(
            ApiGatewayV2Function { Response(NOT_FOUND).body("helloworld") },
            mapOf(
                "rawPath" to "/path",
                "queryStringParameters" to mapOf("query" to "value"),
                "body" to "input body",
                "cookies" to listOf<Pair<String, Any>>(),
                "headers" to mapOf("c" to "d"),
                "requestContext" to mapOf("http" to mapOf("method" to "GET"))
            ),
            mapOf(
                "statusCode" to 404,
                "headers" to emptyMap<String, String>(),
                "multiValueHeaders" to emptyMap<String, String>(),
                "cookies" to emptyList<String>(),
                "body" to "aGVsbG93b3JsZA==",
                "isBase64Encoded" to true
            )
        )
    }
}
