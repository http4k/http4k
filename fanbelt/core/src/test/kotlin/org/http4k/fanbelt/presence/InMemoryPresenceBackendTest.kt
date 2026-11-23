package org.http4k.fanbelt.presence

import org.http4k.fanbelt.presence.memory.InMemoryPresenceBackend

class InMemoryPresenceBackendTest : PresenceBackendContract() {
    override fun backend() = InMemoryPresenceBackend(clock)
}
