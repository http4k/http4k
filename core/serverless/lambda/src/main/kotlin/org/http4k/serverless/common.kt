package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.Filter
import org.http4k.core.with
import org.http4k.lens.RequestKey

val LAMBDA_CONTEXT_KEY = RequestKey.of<Context>("HTTP4K_LAMBDA_CONTEXT")
val LAMBDA_REQUEST_KEY = RequestKey.of<Any>("HTTP4K_LAMBDA_REQUEST")

internal fun AddLambdaContextAndRequest(ctx: Context, request: Any) = Filter { next ->
    {
        next(it.with(LAMBDA_CONTEXT_KEY of ctx, LAMBDA_REQUEST_KEY of request))
    }
}
