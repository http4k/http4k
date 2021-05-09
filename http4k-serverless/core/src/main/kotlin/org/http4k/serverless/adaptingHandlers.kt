package org.http4k.serverless

import org.http4k.format.AutoMarshalling
import org.http4k.format.ServerlessMoshi

/**
 * Use this class to provide a mapping layer between a JSON tree and a custom Serverless Event object.
 */
class AdaptingFunctionHandler<In, Ctx, Out : Any>(
    private val fn: (In, Ctx) -> Out, private val convert: (Map<String, Any>) -> In
) : FunctionHandler<In, Ctx, Out> by object : FunctionHandler<In, Ctx, Out> {
    override fun invoke(`in`: In, ctx: Ctx): Out = fn(`in`, ctx)
} {
    internal fun convert(`in`: Map<String, Any>, ctx: Ctx): Out = fn(convert(`in`), ctx)
}

/**
 * Convenience DSL for constructing a converting FunctionHandler
 */
fun <In, Ctx, Out : Any> FunctionLoader(
    autoMarshalling: AutoMarshalling = ServerlessMoshi,
    makeHandler: (Map<String, String>) -> FunctionHandler<In, Ctx, Out>
): FunctionLoader<Ctx> = FunctionLoader<Ctx> { env ->
    TODO()
//    with(makeHandler(env)) {
//        StreamHandler { inputStream, ctx: Ctx ->
//            autoMarshalling
//                .asFormatString(convert(ServerlessMoshi.asA(inputStream), ctx))
//                .trimStart('"')
//                .trimEnd('"')
//                .byteInputStream()
//        }
//    }
}
