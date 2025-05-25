package org.http4k.server

import org.eclipse.jetty.http.HttpCompliance.RFC7230_LEGACY
import org.http4k.util.InMemoryTest
import org.junit.jupiter.api.Test

class Jetty11DeprecationTest: InMemoryTest {

    @Test
    fun `deprecated things exist in jetty library`() = runBlocking {
        // this is going to be removed at some point and we would like to know that they do
        RFC7230_LEGACY
    }
}
