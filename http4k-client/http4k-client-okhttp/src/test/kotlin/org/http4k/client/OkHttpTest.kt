package org.http4k.client

import org.http4k.server.Jetty

class OkHttpTest : HttpClientContract({ Jetty(it) }, OkHttp(), OkHttp(timeout))