package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.format.AutoMarshalling
import org.http4k.format.AwsLambdaMoshi

/**
 * Convenience DSL for constructing polymorphic AWS FnHandlers
 */
inline fun <reified In : Any, Out : Any> FnLoader(
    autoMarshalling: AutoMarshalling = AwsLambdaMoshi,
    noinline makeHandler: (Map<String, String>) -> FnHandler<In, Context, Out>
) = AutoMarshallingFnLoader(autoMarshalling, In::class, makeHandler)
