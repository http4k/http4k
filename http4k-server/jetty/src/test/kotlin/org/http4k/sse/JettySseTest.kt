package org.http4k.sse

import org.http4k.client.JavaHttpClient
import org.http4k.server.Jetty

class JettySseTest : SseServerContract(::Jetty, JavaHttpClient())
