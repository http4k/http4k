/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import org.http4k.fanbelt.bus.memory.InMemoryBroker
import org.junit.jupiter.api.Assumptions.abort

class InMemoryTransportTest : TransportContract() {
    private val broker = InMemoryBroker()

    override fun transport() = broker.transport()

    override fun dropConnections() = abort<Unit>("the in-memory broker never drops a connection")
}
