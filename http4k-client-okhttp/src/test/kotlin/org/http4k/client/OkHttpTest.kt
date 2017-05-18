package org.http4k.client

import org.http4k.server.Jetty

class OkHttpTest : Http4kClientContract({ Jetty(it) }, OkHttp())
