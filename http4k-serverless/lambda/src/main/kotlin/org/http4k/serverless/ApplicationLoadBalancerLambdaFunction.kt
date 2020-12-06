package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent
import org.http4k.base64Encode
import org.http4k.core.HttpHandler
import org.http4k.core.Response

/**
 * This is the main entry point for lambda invocations coming from an Application LoadBalancer.
 * It uses the local environment to instantiate the HttpHandler which can be used
 * for further invocations.
 */
abstract class ApplicationLoadBalancerLambdaFunction(appLoader: AppLoaderWithContexts)
    : AwsLambdaFunction<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent>(ApplicationLoadBalancerAwsHttpAdapter, appLoader),
    RequestHandler<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    override fun handleRequest(req: ApplicationLoadBalancerRequestEvent, ctx: Context) = handle(req, ctx)
}

object ApplicationLoadBalancerAwsHttpAdapter : AwsHttpAdapter<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {
    override fun invoke(req: ApplicationLoadBalancerRequestEvent, ctx: Context) =
        RequestContent(req.path, req.queryStringParameters, null, req.body, req.isBase64Encoded, req.httpMethod, (req.headers
            ?: emptyMap()).mapValues { listOf(it.value) }, emptyList()).asHttp4k()

    override fun invoke(resp: Response) = ApplicationLoadBalancerResponseEvent().also {
        it.statusCode = resp.status.code
        it.headers = resp.headers.toMap()
        it.body = resp.bodyString().base64Encode()
        it.isBase64Encoded = true
    }
}
