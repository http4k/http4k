package org.http4k.contract.jsonschema.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.jsonschema.v3.SchemaModelNamer
import org.junit.jupiter.api.Test

class SchemaModelNamerTest {
    @Test
    fun `simple namer`() {
        assertThat(SchemaModelNamer.Simple("bob"), equalTo("String"))
    }

    @Test
    fun `full namer`() {
        assertThat(SchemaModelNamer.Full("bob"), equalTo("java.lang.String"))
    }
}
