package org.http4k.connect.amazon.lambda.model

import java.nio.ByteBuffer

data class FunctionPackage(
    val name: Function,
    val handler: FunctionHandler,
    val jar: ByteBuffer,
    val role: Role,
    val environmentProperties: Map<String, String> = mapOf(),
    val timeoutInSeconds: Int = 15
)

data class Function(val value: String)

data class FunctionHandler(val value: String)

data class FunctionDetails(val arn: String, val name: String)

data class Role(val name: String)

enum class LambdaIntegrationType { ApiGatewayRest, ApiGatewayV1, ApiGatewayV2, ApplicationLoadBalancer, Invocation }
