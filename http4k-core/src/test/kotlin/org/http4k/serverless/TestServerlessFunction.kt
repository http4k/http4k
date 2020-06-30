package org.http4k.serverless

import org.http4k.client.ServerForClientContract
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters

object TestServerlessFunction : AppLoader {
    override fun invoke(env: Map<String, String>): HttpHandler =
        DebuggingFilters.PrintRequestAndResponse().then(ServerForClientContract)
}
