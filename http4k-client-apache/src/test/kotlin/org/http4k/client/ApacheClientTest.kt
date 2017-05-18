package org.http4k.client

import org.http4k.server.Jetty

class BasicApacheClientTest : BasicClientContract({ Jetty(it) }, ApacheClient())

class HttpBinApacheHttpTest : Http4kClientContract() {
    override val client = ApacheClient()
}
