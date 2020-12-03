package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent
import org.http4k.core.HttpHandler
import org.http4k.core.Response

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

object ApplicationLoadBalancerAwsHttpAdapter : AwsHttpAdapter<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {
    override fun invoke(req: ApplicationLoadBalancerRequestEvent, ctx: Context) =
        RequestContent(req.path, req.queryStringParameters, null, req.body, req.isBase64Encoded, req.httpMethod, req.headers, emptyList()).asHttp4k()

    override fun invoke(req: Response) = ApplicationLoadBalancerResponseEvent().also {
        it.statusCode = req.status.code
        it.headers = req.headers.toMap()
        it.body = req.bodyString()
    }
}
