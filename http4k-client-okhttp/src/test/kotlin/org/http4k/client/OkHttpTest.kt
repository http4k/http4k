package org.http4k.client

import org.http4k.server.Jetty

class BasicOkHttpTest : BasicClientContract({ Jetty(it) }, OkHttp())

//class HttpBinOkHttpTest : Http4kClientContract() {
//    override val client = OkHttp()
//}