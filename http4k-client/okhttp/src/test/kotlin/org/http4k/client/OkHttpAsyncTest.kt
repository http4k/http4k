package org.http4k.client

import org.http4k.server.SunHttp

class OkHttpAsyncTest : AsyncHttpHandlerContract(::SunHttp, OkHttp(), OkHttp(timeout))
