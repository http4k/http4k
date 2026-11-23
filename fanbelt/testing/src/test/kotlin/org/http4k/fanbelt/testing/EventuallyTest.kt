package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class EventuallyTest {
    @Test
    fun `a list that hasn't filled yet is worth waiting for`() {
        val reads = AtomicInteger()

        val found = eventually { List(reads.getAndIncrement()) { "row" }.single() }

        assertThat(found, equalTo("row"))
    }

    @Test
    fun `an assertion that doesn't hold yet is worth waiting for`() {
        val reads = AtomicInteger()

        eventually { assertThat(reads.incrementAndGet(), equalTo(3)) }

        assertThat(reads.get(), equalTo(3))
    }

    @Test
    fun `an unexpected failure is not waited on`() {
        val reads = AtomicInteger()

        assertThat({
            eventually<Unit> {
                reads.incrementAndGet()
                error("broken")
            }
        }, throws<IllegalStateException>())
        assertThat(reads.get(), equalTo(1))
    }

    @Test
    fun `it gives up in the end`() {
        assertThat({ eventually(Duration.ofMillis(50)) { emptyList<String>().single() } }, throws<NoSuchElementException>())
    }
}
