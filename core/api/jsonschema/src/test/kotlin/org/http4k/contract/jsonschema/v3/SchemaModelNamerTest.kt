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

    @Test
    fun `filter namer`() {
        val namer = SchemaModelNamerChain { if (it == 0) "ZERO" else null }.then(SchemaModelNamer.Simple)
        assertThat(namer(0), equalTo("ZERO"))
        assertThat(namer("bob"), equalTo("String"))
    }
}

class FooBar {
    class BarFoo
}
