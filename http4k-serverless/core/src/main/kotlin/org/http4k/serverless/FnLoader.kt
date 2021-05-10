package org.http4k.serverless

import java.io.InputStream

/**
 * Loads a configured function from the Serverless environment.
 */
fun interface FnLoader<Ctx> : (Map<String, String>) -> FnHandler<InputStream, Ctx, InputStream>

fun <Ctx> FnFilter<InputStream, Ctx, InputStream>.then(fnLoader: FnLoader<Ctx>) = FnLoader { then(fnLoader(it)) }
