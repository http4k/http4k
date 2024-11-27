package org.http4k.connect.mattermost.model

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import dev.forkhandles.values.ofOrNull
import org.junit.jupiter.api.Test

class HexColourTest {

    @Test
    fun `must be hex string`() {
        assertThat(HexColour.ofOrNull(""), absent())
        assertThat(HexColour.ofOrNull("123456"), absent())
        assertThat(HexColour.ofOrNull("#ASDasd"), absent())
        assertThat(HexColour.ofOrNull("#123456"), present())
    }
}
