package org.http4k.tracing.persistence

import org.http4k.tracing.TracePersistence

class InMemoryTracePersistenceTest : TracePersistenceContract {
    override val persistence = TracePersistence.InMemory()
}