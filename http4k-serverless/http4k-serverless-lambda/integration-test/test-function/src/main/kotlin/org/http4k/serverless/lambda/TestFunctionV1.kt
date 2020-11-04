@file:Suppress("unused")

package org.http4k.serverless.lambda

import org.http4k.client.ServerForClientContract
import org.http4k.serverless.ApiGatewayV1LambdaFunction

class TestFunctionV1 : ApiGatewayV1LambdaFunction(ServerForClientContract)
