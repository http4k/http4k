package org.http4k.connect.lmstudio

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.filter.debug
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealLmStudioTest : LmStudioContract {

    init {
        assumeTrue(
            JavaHttpClient()(Request(Method.GET, "http://localhost:1234/")).status.successful,
            "No LmStudio server running"
        )
    }

    override val lmStudio = LmStudio.Http(
        JavaHttpClient().debug(),
    )
}
