package org.http4k.client

import org.http4k.server.ApacheServer

class JettyClientTest : HttpClientContract(::ApacheServer, JettyClient(), JettyClient(requestModifier = timeout))
