package org.http4k.serverless

import org.http4k.format.Moshi

/**
 * Convenience DSL for constructing polymorphic AWS FnHandlers
 */
inline fun <reified In : Any, Ctx, Out : Any> FnLoader(
    noinline makeHandler: (Map<String, String>) -> FnHandler<In, Ctx, Out>
) = AutoMarshallingFnLoader(Moshi, In::class, makeHandler)
