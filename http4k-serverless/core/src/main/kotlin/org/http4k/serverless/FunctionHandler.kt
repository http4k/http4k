package org.http4k.serverless

/**
 * Handler for a Serverless function invocation.
 */
typealias FunctionHandler<In, Ctx, Out> = (In, ctx: Ctx) -> Out
