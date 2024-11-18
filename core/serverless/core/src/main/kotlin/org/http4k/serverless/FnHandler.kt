package org.http4k.serverless

/**
 * Handler for a Serverless function invocation.
 */
fun interface FnHandler<In, Ctx, Out> : (In, Ctx) -> Out

fun interface FnFilter<In, Ctx, Out> : (FnHandler<In, Ctx, Out>) -> FnHandler<In, Ctx, Out> {
    companion object
}

fun <In, Ctx, Out> FnFilter<In, Ctx, Out>.then(next: FnFilter<In, Ctx, Out>): FnFilter<In, Ctx, Out> =
    FnFilter { this(next(it)) }

fun <In, Ctx, Out> FnFilter<In, Ctx, Out>.then(next: FnHandler<In, Ctx, Out>): FnHandler<In, Ctx, Out> =
    FnHandler { `in`, ctx -> this(next)(`in`, ctx) }
