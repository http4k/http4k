package org.http4k.client

import org.http4k.server.ApacheServer

class OkHttpTest : HttpClientContract({ ApacheServer(it) }, OkHttp(), OkHttp(timeout))
