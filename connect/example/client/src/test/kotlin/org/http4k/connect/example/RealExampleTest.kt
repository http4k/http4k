package org.http4k.connect.example

import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach

class RealExampleTest : ExampleContract, PortBasedTest {

    @BeforeEach
    fun setUp() {
        // this should auto-detect any configuration via assume()
        assumeTrue(false)
    }

    override val http = SetHostFrom(Uri.of("http://localhost:9876")).then(JavaHttpClient())
}
