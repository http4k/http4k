package org.http4k.serverless.lambda

import org.http4k.client.ServerForClientContract
import org.http4k.serverless.ApiGatewayV2LambdaFunction

class TestFunctionV2 : ApiGatewayV2LambdaFunction(ServerForClientContract)
