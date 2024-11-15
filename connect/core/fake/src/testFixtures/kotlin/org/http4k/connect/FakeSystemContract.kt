package org.http4k.connect

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.defaultLocalUri
import org.http4k.chaos.defaultPort
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

abstract class FakeSystemContract(private val fake: ChaoticHttpHandler) {
    protected abstract val anyValid: Request

    @Test
    fun `can serve the Chaos API`() {
        val response = fake(Request(GET, "/chaos"))
        assertThat(
            response,
            hasStatus(OK).and(hasContentType(APPLICATION_JSON)).and(hasBody(containsSubstring("http4k Chaos Engine")))
        )
    }

    @Test
    fun `returns error when told to misbehave`() {
        val originalStatus = fake(anyValid).status
        fake.returnStatus(I_M_A_TEAPOT)
        assertThat(fake(anyValid), hasStatus(I_M_A_TEAPOT))
        fake.behave()
        assertThat(fake(anyValid), hasStatus(originalStatus))
    }

    @Test
    fun `default port number is suitably random`() {
        assertThat(fake::class.defaultPort, greaterThan(10000))
        assertThat(fake::class.defaultPort % 100, greaterThan(0))
    }

    @Test
    fun `default local uri`() {
        assertThat(fake::class.defaultLocalUri, equalTo(Uri.of("http://localhost:${fake::class.defaultPort}")))
    }
}
