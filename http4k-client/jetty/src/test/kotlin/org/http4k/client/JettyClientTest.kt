package org.http4k.client

import org.http4k.server.ApacheServer

class JettyClientTest : HttpClientContract({ ApacheServer(it) }, JettyClient(), JettyClient(requestModifier = timeout))
