package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.ClientContext
import com.amazonaws.services.lambda.runtime.CognitoIdentity
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import org.http4k.core.Request
import org.http4k.serverless.LambdaRuntimeAPI.Companion.deadline
import org.http4k.serverless.LambdaRuntimeAPI.Companion.lambdaArn
import org.http4k.serverless.LambdaRuntimeAPI.Companion.requestId
import java.time.Clock
import java.time.Duration.between

/**
 * Custom http4k version of the Lambda Context interface.
 */
class LambdaEnvironmentContext(
    private val req: Request,
    private val env: Map<String, String>,
    private val clock: Clock = Clock.systemUTC()
) : Context {
    override fun getAwsRequestId() = requestId(req)?.toString()

    override fun getLogGroupName() = env["AWS_LAMBDA_LOG_GROUP_NAME"]

    override fun getLogStreamName() = env["AWS_LAMBDA_LOG_STREAM_NAME"]

    override fun getFunctionName() = env["AWS_LAMBDA_FUNCTION_NAME"]

    override fun getFunctionVersion() = env["AWS_LAMBDA_FUNCTION_VERSION"]

    override fun getInvokedFunctionArn() = lambdaArn(req)

    override fun getRemainingTimeInMillis() =
        between(clock.instant(), deadline(req)).toMillis().toInt()

    override fun getMemoryLimitInMB() = env["AWS_LAMBDA_FUNCTION_MEMORY_SIZE"]?.toInt() ?: 0

    override fun getLogger() = object : LambdaLogger {
        override fun log(message: String) = println(message)
        override fun log(message: ByteArray) = println(String(message))
    }

    override fun getIdentity(): CognitoIdentity = throw UnsupportedOperationException("not implemented")

    override fun getClientContext(): ClientContext = throw UnsupportedOperationException("not implemented")
}
