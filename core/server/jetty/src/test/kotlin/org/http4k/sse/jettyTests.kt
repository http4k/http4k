package org.http4k.sse

import org.http4k.client.JavaHttpClient
import org.http4k.server.Jetty
import org.http4k.server.ServerConfig

class JettySseTest : SseServerContract({ Jetty(it, ServerConfig.StopMode.Immediate) }, JavaHttpClient())

class JettyDatastarTest : DatastarServerContract({ Jetty(it, ServerConfig.StopMode.Immediate) }, JavaHttpClient())
