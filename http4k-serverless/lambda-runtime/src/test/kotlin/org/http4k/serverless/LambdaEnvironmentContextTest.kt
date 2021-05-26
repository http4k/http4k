package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant.EPOCH
import java.time.ZoneId
import java.util.UUID

class LambdaEnvironmentContextTest {

    private val ctx = LambdaEnvironmentContext(
        Request(GET, "")
            .header("Lambda-Runtime-Aws-Request-Id", UUID(0,0).toString())
            .header("Lambda-Runtime-Deadline-Ms", "1000")
            .header("Lambda-Runtime-Invoked-Function-Arn", "Arn")
            .header("Lambda-Runtime-Trace-Id", "Trace"),
        mapOf(
            "AWS_LAMBDA_LOG_GROUP_NAME" to "LogGroup",
            "AWS_LAMBDA_LOG_STREAM_NAME" to "LogStream",
            "AWS_LAMBDA_FUNCTION_NAME" to "Name",
            "AWS_LAMBDA_FUNCTION_VERSION" to "Version",
            "AWS_LAMBDA_FUNCTION_MEMORY_SIZE" to "128"
            ),
        Clock.fixed(EPOCH, ZoneId.of("UTC"))
    )

    @Test
    fun getAwsRequestId() {
        assertThat(ctx.awsRequestId, equalTo("00000000-0000-0000-0000-000000000000"))
    }

    @Test
    fun getLogGroupName() {
        assertThat(ctx.logGroupName, equalTo("LogGroup"))
    }

    @Test
    fun getLogStreamName() {
        assertThat(ctx.logStreamName, equalTo("LogStream"))
    }

    @Test
    fun getFunctionName() {
        assertThat(ctx.functionName, equalTo("Name"))
    }

    @Test
    fun getFunctionVersion() {
        assertThat(ctx.functionVersion, equalTo("Version"))
    }

    @Test
    fun getInvokedFunctionArn() {
        assertThat(ctx.invokedFunctionArn, equalTo("Arn"))
    }

    @Test
    fun getRemainingTimeInMillis() {
        assertThat(ctx.remainingTimeInMillis, equalTo(1000))
    }

    @Test
    fun getMemoryLimitInMB() {
        assertThat(ctx.memoryLimitInMB, equalTo(128))
    }

    @Test
    fun getIdentity() {
        assertThat({ ctx.identity }, throws<UnsupportedOperationException>())
    }

    @Test
    fun getClientContext() {
        assertThat({ ctx.clientContext }, throws<UnsupportedOperationException>())
    }
}
