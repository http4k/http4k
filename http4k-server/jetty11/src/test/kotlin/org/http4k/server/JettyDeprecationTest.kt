package org.http4k.server

import org.eclipse.jetty.http.HttpCompliance.RFC7230_LEGACY
import org.junit.jupiter.api.Test

class JettyDeprecationTest {

    @Test
    fun `deprecated things exist in jetty library`() {
         // this is going to be removed at some point and we would like to know that they do
        RFC7230_LEGACY
    }
}
