package org.http4k.client

import org.http4k.server.SunHttp

class JettyClientAsyncTest : AsyncHttpHandlerContract(::SunHttp, JettyClient(), JettyClient(requestModifier = timeout))
