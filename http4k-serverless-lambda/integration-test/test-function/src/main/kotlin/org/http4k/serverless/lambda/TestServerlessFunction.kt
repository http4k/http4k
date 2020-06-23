@file:Suppress("unused")

package org.http4k.serverless.lambda

import org.http4k.client.ServerForClientContract
import org.http4k.core.HttpHandler
import org.http4k.serverless.AppLoader

object TestServerlessFunction : AppLoader {
    override fun invoke(env: Map<String, String>): HttpHandler = ServerForClientContract
}
