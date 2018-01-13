package org.http4k.client

import org.http4k.server.SunHttp

class OkHttpAsyncTest : Http4kAsyncClientContract({ SunHttp(it) }, OkHttp(), OkHttp(timeout))