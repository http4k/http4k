package org.http4k.server

import org.http4k.client.JavaHttpClient
import org.http4k.sse.SseServerContract

class HelidonSseTest : SseServerContract({ Helidon(it, ServerConfig.StopMode.Immediate) }, JavaHttpClient())
