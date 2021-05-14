package org.http4k.testing

import com.amazonaws.services.lambda.runtime.ClientContext
import com.amazonaws.services.lambda.runtime.CognitoIdentity
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.serverless.FnLoader
import org.http4k.serverless.AwsEnvironment.AWS_LAMBDA_FUNCTION_NAME
import java.util.UUID

/**
 * Simulates the API of calling AWS Lambda function directly
 */
object FakeAwsLambda {
    operator fun invoke(fn: FnLoader<Context>, env: Map<String, String> = System.getenv()) =
        CatchAll()
            .then(
                routes("/2015-03-31/functions/{name}/invocations" bind Method.POST to {
                    val name = it.path("name") ?: "unknown"
                    val customEnv = env + (AWS_LAMBDA_FUNCTION_NAME to name)
                    Response(OK).body(fn(customEnv)(it.body.stream, FakeContext(name)))
                })
            )
}

internal fun FakeContext(name: String) = object : Context {
    override fun getAwsRequestId() = UUID.randomUUID().toString()

    override fun getLogGroupName() = "logGroupName"

    override fun getLogStreamName() = "logStreamName"

    override fun getFunctionName() = name

    override fun getFunctionVersion() = "latest"

    override fun getInvokedFunctionArn() = "arn:aws:lambda:us-east-1:000000000000:function:$name"

    override fun getRemainingTimeInMillis() = Int.MAX_VALUE

    override fun getMemoryLimitInMB() = Int.MAX_VALUE

    override fun getLogger() = object : LambdaLogger {
        override fun log(message: String) = println(message)
        override fun log(message: ByteArray) = println(String(message))
    }

    override fun getIdentity(): CognitoIdentity = error("not implemented")

    override fun getClientContext(): ClientContext = error("not implemented")
}
