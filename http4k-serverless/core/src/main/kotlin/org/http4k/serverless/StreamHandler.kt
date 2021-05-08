package org.http4k.serverless

import java.io.InputStream

/**
 * Low level function request handler which relies only on Streams.
 */
fun interface StreamHandler<Ctx> {
    operator fun invoke(inputStream: InputStream, ctx: Ctx): InputStream

    companion object
}
