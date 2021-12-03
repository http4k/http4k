package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.Filter
import org.http4k.core.RequestContexts

const val LAMBDA_CONTEXT_KEY = "HTTP4K_LAMBDA_CONTEXT"
const val LAMBDA_REQUEST_KEY = "HTTP4K_LAMBDA_REQUEST"

internal fun AddLambdaContextAndRequest(ctx: Context, request: Any, contexts: RequestContexts) = Filter { next ->
    {
        contexts[it][LAMBDA_CONTEXT_KEY] = ctx
        contexts[it][LAMBDA_REQUEST_KEY] = request
        next(it)
    }
}
