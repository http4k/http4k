package org.http4k.serverless

import org.http4k.format.AutoMarshalling
import java.io.InputStream
import kotlin.reflect.KClass

/**
 * Convenience DSL for constructing a converting polymorphic FnHandler
 */
class AutoMarshallingFnLoader<In : Any, Ctx, Out : Any>(
    private val autoMarshalling: AutoMarshalling,
    private val clazz: KClass<In>,
    private val makeHandler: (Map<String, String>) -> FnHandler<In, Ctx, Out>
) : FnLoader<Ctx> {
    override fun invoke(env: Map<String, String>): FnHandler<InputStream, Ctx, InputStream> {
        val receiver = makeHandler(env)
        return FnHandler { inputStream, ctx: Ctx ->
            autoMarshalling
                .asFormatString(receiver(autoMarshalling.asA(inputStream, clazz), ctx))
                .trimStart('"')
                .trimEnd('"')
                .byteInputStream()
        }
    }
}
