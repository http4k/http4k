package org.http4k.server

import org.http4k.client.JavaHttpClient

class JettyTest : ServerContract(::Jetty, JavaHttpClient())
