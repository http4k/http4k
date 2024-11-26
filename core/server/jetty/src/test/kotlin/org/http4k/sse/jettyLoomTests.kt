package org.http4k.sse

import org.http4k.client.JavaHttpClient
import org.http4k.server.JettyLoom
import org.http4k.server.ServerConfig.StopMode.Immediate

class JettyLoomTests : SseServerContract({ JettyLoom(it, Immediate) }, JavaHttpClient())

class JettyLoomDatastarTest : DatastarServerContract({ JettyLoom(it, Immediate) }, JavaHttpClient())
