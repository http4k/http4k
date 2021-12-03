package org.http4k.serverless

import com.google.cloud.functions.Context
import org.http4k.format.AutoMarshalling
import org.http4k.format.Moshi

/**
 * Convenience DSL for constructing polymorphic GCF FnHandlers
 */
inline fun <reified In : Any, Out : Any> FnLoader(
    autoMarshalling: AutoMarshalling = Moshi,
    noinline makeHandler: (Map<String, String>) -> FnHandler<In, Context, Out>
) = AutoMarshallingFnLoader(autoMarshalling, In::class, makeHandler)
