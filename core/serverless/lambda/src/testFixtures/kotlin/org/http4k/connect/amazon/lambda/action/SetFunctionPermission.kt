package org.http4k.connect.amazon.lambda.action

import com.fasterxml.jackson.annotation.JsonProperty
import org.http4k.connect.amazon.lambda.LambdaAction
import org.http4k.connect.amazon.lambda.LambdaJackson.auto
import org.http4k.connect.kClass
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with

class SetFunctionPermission(private val functionArn: String, private val permission: Permission) : LambdaAction<Unit>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/2015-03-31/functions/${functionArn}/policy")
        .with(Body.auto<Permission>().toLens() of Permission.invokeFromApiGateway)
}

data class Permission(
    @JsonProperty("Action") val action: String = "lambda:InvokeFunction",
    @JsonProperty("Principal") val principal: String,
    @JsonProperty("StatementId") val statementId: String
) {
    companion object {
        val invokeFromApiGateway = Permission(principal = "apigateway.amazonaws.com", statementId = "apigateway")
    }
}
