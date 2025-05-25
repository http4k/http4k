package org.http4k.serverless

import java.io.InputStream

/**
 * Loads a configured function from the Serverless environment.
 */
typealias FnLoader<Ctx> = suspend (Map<String, String>) -> FnHandler<InputStream, Ctx, InputStream>

fun <Ctx> FnFilter<InputStream, Ctx, InputStream>.then(fnLoader: FnLoader<Ctx>) = object : FnLoader<Ctx> {
    override suspend fun invoke(it: Map<String, String>): FnHandler<InputStream, Ctx, InputStream> = then(fnLoader(it))
}
