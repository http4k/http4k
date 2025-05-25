package org.http4k.config

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class PortTest {

    @Test
    fun `random port value`() = runBlocking {
        assertThat(Port.RANDOM, equalTo(Port(0)))
    }

    @Test
    fun `max port value`() = runBlocking {
        Port(65535)
        assertThat({ Port(65536) }, throws<IllegalArgumentException>())
    }
}
