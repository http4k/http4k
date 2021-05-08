package org.http4k.serverless

/**
 * Loads a configured function from the Serverless environment.
 */
typealias FunctionLoader<Ctx> = (Map<String, String>) -> StreamHandler<Ctx>
