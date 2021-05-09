package org.http4k.serverless

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

