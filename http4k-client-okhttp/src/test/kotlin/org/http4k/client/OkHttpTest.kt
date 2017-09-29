package org.http4k.client

import org.http4k.server.SunHttp

class OkHttpTest : Http4kClientContract({ SunHttp(it) }, OkHttp(), OkHttp(timeout))
