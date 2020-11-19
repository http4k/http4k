@file:Suppress("unused")

package org.http4k.serverless.lambda

import org.http4k.client.ServerForClientContract
import org.http4k.serverless.DirectLambdaFunction

class TestFunctionDirect : DirectLambdaFunction(ServerForClientContract)
