package org.http4k.client

import org.http4k.server.SunHttp

class JettyClientTest : HttpClientContract({ SunHttp(it) }, JettyClient(), JettyClient(requestModifier = timeout) )