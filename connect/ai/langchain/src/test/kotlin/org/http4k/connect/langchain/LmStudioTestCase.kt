package org.http4k.connect.langchain

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions

abstract class LmStudioTestCase: PortBasedTest {
    init {
        Assumptions.assumeTrue(
            JavaHttpClient()(Request(Method.GET, "http://localhost:1234/")).status.successful,
            "No LmStudio server running"
        )
    }
}
