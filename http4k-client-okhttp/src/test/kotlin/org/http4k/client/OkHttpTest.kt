package org.http4k.client

import org.http4k.server.SunHttp

class OkHttpTest : HttpClientContract({ SunHttp(it) }, OkHttp(), OkHttp(timeout))