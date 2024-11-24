package org.http4k.sse

import org.http4k.client.JavaHttpClient
import org.http4k.server.JettyLoom
import org.http4k.server.ServerConfig

class JettyLoomSseTest : SseServerContract({ JettyLoom(it, ServerConfig.StopMode.Immediate) }, JavaHttpClient())
