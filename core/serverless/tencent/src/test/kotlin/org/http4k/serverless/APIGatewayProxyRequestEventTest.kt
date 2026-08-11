package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent
import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent.ProxyRequestContext
import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent.RequestIdentity
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson.asA
import org.http4k.format.Jackson.asFormatString
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * The SCF runtime binds the incoming event JSON onto this bean reflectively, so the field names in the
 * approved JSON are a wire contract - including the irregularly-cased setisBase64Encoded accessor.
 */
@ExtendWith(JsonApprovalTest::class)
class APIGatewayProxyRequestEventTest {

    @Test
    fun `APIGateway proxy request event`(approver: Approver) {
        approver.assertRoundtrips(APIGatewayProxyRequestEvent().apply {
            httpMethod = "POST"
            path = "/test/value"
            body = "input body"
            headers = mapOf("accept" to "text/html", "content-type" to "application/json")
            pathParameters = mapOf("path" to "value")
            queryStringParameters = mapOf("query" to "value")
            headerParameters = mapOf("headerParam" to "hp")
            stageVariables = mapOf("stage" to "release")
            queryString = mapOf("query" to "value")
            setisBase64Encoded(true)
            requestContext = ProxyRequestContext().apply {
                serviceId = "service-abc"
                path = "/test/{path}"
                httpMethod = "POST"
                requestId = "req-1"
                identity = RequestIdentity().apply { secretId = "abcdefg" }
                sourceIp = "10.0.0.1"
                stage = "release"
                isBase64Encoded = false
            }
        })
    }
}

private inline fun <reified T : Any> Approver.assertRoundtrips(input: T) {
    val asString = asFormatString(input)
    assertApproved(Response(OK).with(CONTENT_TYPE of APPLICATION_JSON).body(asString))
    assertThat(asFormatString(asA<T>(asString)), equalTo(asString))
}
