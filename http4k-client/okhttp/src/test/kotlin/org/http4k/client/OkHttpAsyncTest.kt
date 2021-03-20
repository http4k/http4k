package org.http4k.client

import org.http4k.server.SunHttp

class OkHttpAsyncTest : AsyncHttpClientContract(::SunHttp, OkHttp(), OkHttp(timeout))
