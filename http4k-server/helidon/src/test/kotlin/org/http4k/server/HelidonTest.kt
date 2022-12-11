package org.http4k.server

import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Method.PURGE

class HelidonTest : ServerContract(::Helidon, ApacheClient(),
    Method.values().filter { it != PURGE }.toTypedArray())
