package org.http4k.client

import org.http4k.server.SunHttp

class ApacheClientStreamingTest : Http4kClientContract({ SunHttp(it) }, ApacheClient(bodyMode = ResponseBodyMode.Stream))