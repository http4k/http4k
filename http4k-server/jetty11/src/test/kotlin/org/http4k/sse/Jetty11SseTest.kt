package org.http4k.sse

import org.http4k.client.JavaHttpClient
import org.http4k.server.Jetty11

class Jetty11SseTest : SseServerContract(::Jetty11, JavaHttpClient())

