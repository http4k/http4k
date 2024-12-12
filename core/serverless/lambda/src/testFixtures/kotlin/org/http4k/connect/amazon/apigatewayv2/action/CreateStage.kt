package org.http4k.connect.amazon.apigatewayv2.action

import org.http4k.connect.amazon.apigatewayv2.ApiGatewayJackson.auto
import org.http4k.connect.amazon.apigatewayv2.AwsApiGatewayV2Action
import org.http4k.connect.amazon.apigatewayv2.model.ApiId
import org.http4k.connect.amazon.apigatewayv2.model.Stage
import org.http4k.connect.kClass
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with

class CreateStage(private val apiId: ApiId, private val stage: Stage) : AwsApiGatewayV2Action<Unit>(kClass()) {
    private val createStageLens =
        Body.auto<Stage>(contentType = ContentType.APPLICATION_JSON.withNoDirectives()).toLens()

    override fun toRequest() = Request(Method.POST, "/v2/apis/${apiId.value}/stages")
        .with(createStageLens of stage)
}
