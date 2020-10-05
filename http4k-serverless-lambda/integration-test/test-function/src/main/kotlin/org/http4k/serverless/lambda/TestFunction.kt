package org.http4k.serverless.lambda

import org.http4k.client.ServerForClientContract
import org.http4k.serverless.ApiGatewayV1LambdaFunction

class TestFunction : ApiGatewayV1LambdaFunction(ServerForClientContract)
