package org.http4k.serverless

/**
 * Loads a configured function from the Serverless environment.
 */
fun interface FunctionLoader<Ctx> : (Map<String, String>) -> StreamHandler<Ctx>
