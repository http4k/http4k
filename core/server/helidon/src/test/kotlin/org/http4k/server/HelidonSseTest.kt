package org.http4k.server

import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.SseServerContract

class HelidonSseTest : SseServerContract({ Helidon(it, Immediate) })
