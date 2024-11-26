package org.http4k.server

import org.http4k.client.JavaHttpClient
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.DatastarServerContract

class HelidonDatastarTest : DatastarServerContract({ Helidon(it, Immediate) }, JavaHttpClient())
