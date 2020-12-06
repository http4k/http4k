@file:Suppress("unused")

package org.http4k.serverless.lambda

import org.http4k.client.ServerForClientContract
import org.http4k.core.Filter
import org.http4k.core.then
import org.http4k.serverless.InvocationLambdaFunction

class TestFunctionInvocation : InvocationLambdaFunction(
    Filter { next -> { next(it.uri(it.uri.path("/echo"))) } }
        .then(ServerForClientContract)
)
