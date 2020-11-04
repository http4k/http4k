package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context

class LambdaContextMock(private val functionName: String = "LambdaContextMock") : Context {
    override fun getFunctionName() = functionName
    override fun getAwsRequestId() = TODO("LambdaContextMock")
    override fun getLogStreamName() = TODO("LambdaContextMock")
    override fun getInvokedFunctionArn() = TODO("LambdaContextMock")
    override fun getLogGroupName() = TODO("LambdaContextMock")
    override fun getFunctionVersion() = TODO("LambdaContextMock")
    override fun getIdentity() = TODO("LambdaContextMock")
    override fun getClientContext() = TODO("LambdaContextMock")
    override fun getRemainingTimeInMillis() = TODO("LambdaContextMock")
    override fun getLogger() = TODO("LambdaContextMock")
    override fun getMemoryLimitInMB() = TODO("LambdaContextMock")
}
