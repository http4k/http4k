package org.http4k.serverless

/**
 * Handler for a Serverless function invocation.
 */
interface FunctionHandler<In, Ctx, Out> {
    operator fun invoke(`in`: In, ctx: Ctx): Out

    companion object
}
