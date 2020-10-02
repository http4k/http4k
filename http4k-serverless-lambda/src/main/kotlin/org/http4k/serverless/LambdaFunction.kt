package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.filter.ServerFilters.InitialiseRequestContext

const val LAMBDA_CONTEXT_KEY = "HTTP4K_LAMBDA_CONTEXT"
const val LAMBDA_REQUEST_KEY = "HTTP4K_LAMBDA_REQUEST"

/**
 * This is the main entry point for the lambda. It uses the local environment
 * to instantiate the Http4k handler which can be used for further invocations.
 */
open class LambdaFunction(appLoader: AppLoaderWithContexts) {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })

    constructor(input: HttpHandler) : this(AppLoader { input })

    @Deprecated("This reflection based implementation will be removed in future version. Use class based extension approach instead.")
    constructor(env: Map<String, String> = System.getenv()) : this(AppLoaderWithContexts { _, contexts -> BootstrapAppLoader(env, contexts) })

    private val contexts = RequestContexts()
    private val app = appLoader(System.getenv(), contexts)

    fun handle(request: APIGatewayProxyRequestEvent, lambdaContext: Context? = null) = handle(ApiGatewayV1AwsHttpAdapter, lambdaContext, request)
    fun handle(request: APIGatewayV2HTTPEvent, lambdaContext: Context? = null) = handle(ApiGatewayV2AwsHttpAdapter, lambdaContext, request)
    fun handle(request: ApplicationLoadBalancerRequestEvent, lambdaContext: Context? = null) = handle(ApplicationLoadBalancerAwsHttpAdapter, lambdaContext, request)

    private fun <Req : Any, Resp> handle(awsHttpAdapter: AwsHttpAdapter<Req, Resp>, lambdaContext: Context?, request: Req): Resp =
        awsHttpAdapter(InitialiseRequestContext(contexts).then(AddLambdaContextAndRequest(lambdaContext, request, contexts)).then(app)(awsHttpAdapter(request)))
}

internal fun AddLambdaContextAndRequest(lambdaContext: Context?, request: Any, contexts: RequestContexts) = Filter { next ->
    {
        lambdaContext?.apply { contexts[it][LAMBDA_CONTEXT_KEY] = lambdaContext }
        contexts[it][LAMBDA_REQUEST_KEY] = request
        next(it)
    }
}
