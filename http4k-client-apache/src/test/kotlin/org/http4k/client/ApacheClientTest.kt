package org.http4k.client

import org.http4k.server.Jetty

class ApacheClientTest : Http4kClientContract({ Jetty(it) }, ApacheClient())
