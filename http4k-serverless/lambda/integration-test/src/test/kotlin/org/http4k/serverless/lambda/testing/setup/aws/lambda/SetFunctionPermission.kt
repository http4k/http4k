package org.http4k.serverless.lambda.testing.setup.aws.lambda

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.kClass
import org.http4k.serverless.lambda.testing.setup.aws.lambda.LambdaJackson.auto

class SetFunctionPermission(private val functionArn: String, private val permission: Permission)
    : LambdaAction<Unit>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/2015-03-31/functions/${functionArn}/policy")
        .with(Body.auto<Permission>().toLens() of Permission.invokeFromApiGateway)
}
