package org.http4k.serverless.lambda

import org.http4k.format.AutoMarshalling
import org.http4k.format.AwsLambdaMoshi
import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader

/**
 * Convenience DSL for constructing a converting polymorphic FunctionHandler
 */
inline fun <reified In, Ctx, Out : Any> FnLoader(
    autoMarshalling: AutoMarshalling = AwsLambdaMoshi,
    crossinline makeHandler: (Map<String, String>) -> FnHandler<In, Ctx, Out>
): FnLoader<Ctx> = FnLoader<Ctx> { env ->
    with(makeHandler(env)) {
        FnHandler { inputStream, ctx: Ctx ->
            autoMarshalling
                .asFormatString(this(AwsLambdaMoshi.asA(inputStream), ctx))
                .trimStart('"')
                .trimEnd('"')
                .byteInputStream()
        }
    }
}
