package org.http4k.sse

import org.http4k.client.JavaHttpClient
import org.http4k.server.JettyLoom

class JettyLoomSseTest : SseServerContract(::JettyLoom, JavaHttpClient())
