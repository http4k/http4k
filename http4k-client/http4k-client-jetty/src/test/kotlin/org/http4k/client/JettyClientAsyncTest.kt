package org.http4k.client

import org.http4k.server.SunHttp

class JettyClientAsyncTest : AsyncHttpClientContract({ SunHttp(it) }, JettyClient(), JettyClient(requestModifier = timeout))