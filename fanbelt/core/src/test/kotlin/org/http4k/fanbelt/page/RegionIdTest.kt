package org.http4k.fanbelt.page

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class RegionIdTest {
    @Test
    fun `a region id is a letter then letters, digits, - or _`() {
        assertThat(RegionId.of("row-1_a").value, equalTo("row-1_a"))
    }

    @Test
    fun `anything else is refused, so a region can't be built with it`() {
        listOf("", "1row", "-row", "a b", "a\"b").forEach { bad ->
            assertThat(bad, { RegionId.of(bad) }, throws<IllegalArgumentException>())
        }
    }
}
