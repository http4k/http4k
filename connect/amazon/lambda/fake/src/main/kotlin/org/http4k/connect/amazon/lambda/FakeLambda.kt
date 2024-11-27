package org.http4k.connect.amazon.lambda

import com.amazonaws.services.lambda.runtime.ClientContext
import com.amazonaws.services.lambda.runtime.CognitoIdentity
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.serverless.AwsEnvironment.AWS_LAMBDA_FUNCTION_NAME
import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader
import java.io.InputStream
import java.time.Clock
import java.util.UUID

class FakeLambda(
    fnLoader: FnLoader<Context>,
    private val clock: Clock = Clock.systemUTC(),
    private val env: Map<String, String> = System.getenv()
) : ChaoticHttpHandler() {

    override val app = routes("/2015-03-31/functions/{name}/invocations" bind POST to { req ->
        val name = req.path("name") ?: "unknown"
        val customEnv = env + (AWS_LAMBDA_FUNCTION_NAME to name)
        Response(OK).body(
            fnLoader(customEnv)(
                ByteBufferBackedInputStream(req.body.payload),
                FakeContext(name)
            )
        )
    })

    /**
     * Convenience function to get Lambda client
     */
    fun client() = Lambda.Http(
        Region.of("ldn-north-1"),
        { AwsCredentials("accessKey", "secret") }, this, clock
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

fun main() {
    FakeLambda(FnLoader {
        FnHandler { i: InputStream, _: Context -> i }
    }).start()
}
