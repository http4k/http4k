package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.toUrlFormEncoded

/**
 * This is the main entry point for lambda invocations coming from an Application LoadBalancer.
 * It uses the local environment to instantiate the HttpHandler which can be used
 * for further invocations.
 */
abstract class ApplicationLoadBalancerLambdaFunction(appLoader: AppLoaderWithContexts)
    : AwsLambdaFunction<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent>(ApplicationLoadBalancerAwsHttpAdapter, appLoader) {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    override fun handleRequest(req: ApplicationLoadBalancerRequestEvent, ctx: Context) = handle(req, ctx)
}

internal object ApplicationLoadBalancerAwsHttpAdapter : AwsHttpAdapter<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {
    override fun invoke(req: ApplicationLoadBalancerRequestEvent) = (req.headers ?: emptyMap()).toList().fold(
        Request(Method.valueOf(req.httpMethod), req.uri())
            .body(req.body?.let(::MemoryBody) ?: Body.EMPTY)) { memo, (first, second) ->
        memo.header(first, second)
    }

    override fun invoke(req: Response) = ApplicationLoadBalancerResponseEvent().also {
        it.statusCode = req.status.code
        it.headers = req.headers.toMap()
        it.body = req.bodyString()
    }

    private fun ApplicationLoadBalancerRequestEvent.uri() = Uri.of(path ?: "").query((queryStringParameters
        ?: emptyMap()).toList().toUrlFormEncoded())
}
