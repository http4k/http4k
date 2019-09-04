package org.http4k.client

import org.http4k.server.Jetty

class JettyClientTest : HttpClientContract({ Jetty(it) }, JettyClient(), JettyClient(requestModifier = timeout))