package org.http4k.client

import org.http4k.core.BodyMode
import org.http4k.server.SunHttp

class JettyClientAsyncStreamingTest : AsyncHttpHandlerContract(::SunHttp, JettyClient(bodyMode = BodyMode.Stream),
    JettyClient(bodyMode = BodyMode.Stream, requestModifier = timeout))
