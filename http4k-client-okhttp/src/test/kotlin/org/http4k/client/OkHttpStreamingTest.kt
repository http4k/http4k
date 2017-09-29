package org.http4k.client

import org.http4k.server.SunHttp

class OkHttpStreamingTest : Http4kClientContract({ SunHttp(it) }, OkHttp(bodyMode = ResponseBodyMode.Stream),
    OkHttp(timeout, bodyMode = ResponseBodyMode.Stream))