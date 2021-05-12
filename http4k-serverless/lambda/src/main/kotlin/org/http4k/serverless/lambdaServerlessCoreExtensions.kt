package org.http4k.serverless

import org.http4k.format.AutoMarshalling
import org.http4k.format.AwsLambdaMoshi

/**
 * Convenience DSL for constructing a converting polymorphic FnHandler
 */
inline fun <reified In, Ctx, Out : Any> FnLoader(
    autoMarshalling: AutoMarshalling = AwsLambdaMoshi,
    crossinline makeHandler: (Map<String, String>) -> FnHandler<In, Ctx, Out>
) = AutoMarshallingFnLoader(autoMarshalling, makeHandler)

