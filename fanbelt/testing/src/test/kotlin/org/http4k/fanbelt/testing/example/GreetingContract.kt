package org.http4k.fanbelt.testing.example

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.Verdict.Accepted
import org.http4k.fanbelt.testing.Verdict.Denied
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test

interface GreetingDriver {
    fun visitor(name: String): Visitor
}

interface Visitor {
    fun watches()
    fun says(text: String): Verdict
    fun clears(): Verdict
    fun sees(): String?
}

interface GreetingContract {
    val driver: GreetingDriver

    @Test
    fun `a greeting appears live for everyone watching`() {
        val ann = driver.visitor("ann")
        val bob = driver.visitor("bob")
        bob.watches()

        assertThat(ann.says("hello"), equalTo(Accepted))

        eventually { assertThat(bob.sees(), equalTo("hello")) }
    }

    @Test
    fun `a blank greeting is rejected, and nobody sees anything`() {
        val ann = driver.visitor("ann")
        val bob = driver.visitor("bob")
        bob.watches()

        assertThat(ann.says("   "), equalTo(Verdict.Rejected("Say something")))

        assertThat(bob.sees(), absent())
    }

    @Test
    fun `only whoever set the greeting can clear it`() {
        val ann = driver.visitor("ann")
        val bob = driver.visitor("bob")
        ann.says("hello")

        assertThat(bob.clears(), equalTo(Denied))
        assertThat(ann.clears(), equalTo(Accepted))

        eventually { assertThat(bob.sees(), absent()) }
    }
}
