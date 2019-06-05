package org.http4k.contract.openapi.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ModelNamerTest {
    @Test
    fun `simple namer`() {
        assertThat(ModelNamer.Simple("bob"), equalTo("String"))
    }

    @Test
    fun `full namer`() {
        assertThat(ModelNamer.Full("bob"), equalTo("java.lang.String"))
    }
}