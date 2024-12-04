package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.events.ProtocolStatus
import org.junit.jupiter.api.Test

class ProtocolStatusTest {
    private val v1 = ProtocolStatus(1, "bob", true)
    private val v1Alt = ProtocolStatus(1, "sue", true)
    private val v2 = ProtocolStatus(2, "doo", true)

    @Test
    fun `equality does not include description`() {
        assertThat(v1 == v1Alt, equalTo(true))
        assertThat(v1 == v2, equalTo(false))
    }

    @Test
    fun `hashcode does not include description`() {
        assertThat(v1.hashCode() == v1Alt.hashCode(), equalTo(true))
        assertThat(v1.hashCode() == v2.hashCode(), equalTo(false))
    }
}
