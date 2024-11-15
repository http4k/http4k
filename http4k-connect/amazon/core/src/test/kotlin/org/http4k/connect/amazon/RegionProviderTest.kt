package org.http4k.connect.amazon

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.core.model.Region
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegionProviderTest {

    @Test
    fun `second element has region`() {
        val chain = RegionProvider { null } orElse RegionProvider { Region.CA_CENTRAL_1 }

        assertThat(
            chain.invoke(),
            equalTo(Region.CA_CENTRAL_1)
        )
    }

    @Test
    fun `first element has region`() {
        val chain = RegionProvider { Region.US_EAST_1 } orElse RegionProvider { Region.CA_CENTRAL_1 }

        assertThat(
            chain.invoke(),
            equalTo(Region.US_EAST_1)
        )
    }

    @Test
    fun `no element has region`() {
        val chain = RegionProvider { null } orElse RegionProvider { null }

        assertThat(
            chain.invoke(),
            absent()
        )
    }

    @Test
    fun `no element has region - thrown`() {
        val chain = RegionProvider { null } orElse RegionProvider { null }

        assertThrows<IllegalArgumentException>(chain::orElseThrow)
    }
}
