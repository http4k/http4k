package org.http4k.serverless

import org.http4k.client.ServerForClientContract
import org.http4k.core.HttpHandler

object TestServerlessFunction : AppLoader {
    override fun invoke(env: Map<String, String>): HttpHandler = ServerForClientContract
}
