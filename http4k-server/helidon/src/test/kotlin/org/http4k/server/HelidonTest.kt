package org.http4k.server

import org.http4k.client.ApacheClient
import org.junit.jupiter.api.Disabled

@Disabled
class HelidonTest : ServerContract(::Helidon, ApacheClient())
