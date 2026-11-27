package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds
import java.util.concurrent.atomic.AtomicInteger

class EventuallyTest {
    private fun <T> quickly(check: () -> T) = eventually(within = ofSeconds(5), interval = ofMillis(1), check = check)

    @Test
    fun `a list that hasn't filled yet is worth waiting for`() {
        val reads = AtomicInteger()

        val found = quickly { List(reads.getAndIncrement()) { "row" }.single() }

        assertThat(found, equalTo("row"))
    }

    @Test
    fun `an assertion that doesn't hold yet is worth waiting for`() {
        val reads = AtomicInteger()

        quickly { assertThat(reads.incrementAndGet(), equalTo(3)) }

        assertThat(reads.get(), equalTo(3))
    }

    @Test
    fun `any exception is worth waiting for`() {
        val reads = AtomicInteger()

        quickly { check(reads.incrementAndGet() == 3) { "broken" } }

        assertThat(reads.get(), equalTo(3))
    }

    @Test
    fun `it gives up in the end`() {
        assertThat({ eventually(within = ofMillis(20), interval = ofMillis(1)) { emptyList<String>().single() } }, throws<NoSuchElementException>())
    }
}
