package org.http4k.serverless

import org.http4k.client.ServerForClientContract
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse

object TestServerlessFunction : AppLoader {
    override fun invoke(env: Map<String, String>) = PrintRequestAndResponse().then(ServerForClientContract)
}
