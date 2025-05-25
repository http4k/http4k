package org.http4k.db

import org.http4k.db.Transactor.Mode.ReadOnly
import org.http4k.db.Transactor.Mode.ReadWrite
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InMemoryTransactorTest {

    @Test
    fun `performs read operation`() = runBlocking {
        val transactor = InMemoryTransactor("hello")

        val result = transactor.perform(ReadOnly) { it }

        expectThat(result).isEqualTo("hello")
    }

    @Test
    fun `performs write operation`() = runBlocking {
        val transactor = InMemoryTransactor("hello")

        val result = transactor.perform(ReadWrite) { it }

        expectThat(result).isEqualTo("hello")
    }
}
