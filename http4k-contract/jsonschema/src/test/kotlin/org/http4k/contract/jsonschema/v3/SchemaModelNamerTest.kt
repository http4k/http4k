package org.http4k.contract.jsonschema.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
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

    @Test
    fun `canonical namer`() {
        assertThat(SchemaModelNamer.Canonical(FooBar.BarFoo()), equalTo("org.http4k.contract.jsonschema.v3.FooBar.BarFoo"))
    }
}

class FooBar {
    class BarFoo
}
