package org.http4k.server

import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Method.PURGE
import org.http4k.filter.debug
import org.junit.jupiter.api.Disabled

@Disabled
class HelidonTest : ServerContract(::Helidon, ApacheClient())
