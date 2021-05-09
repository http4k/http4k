package org.http4k.serverless

/**
 * Handler for a Serverless function invocation.
 */
fun interface FunctionHandler<In, Ctx, Out> : (In, Ctx) -> Out
