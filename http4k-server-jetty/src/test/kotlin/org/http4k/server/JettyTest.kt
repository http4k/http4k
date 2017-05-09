package org.http4k.server

import org.http4k.client.ApacheClient

class JettyTest : ServerContract(::Jetty, ApacheClient())