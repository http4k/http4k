package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class Values4kExtensionsKtTest {

    class Foo private constructor(value: Int) : IntValue(value) {
        companion object : IntValueFactory<Foo>(::Foo)
    }

    @Test
    fun ofOrLensFailure() {
        val rawLens = Path.of("foo")

        assertThat(rawLens.ofOrLensFailure(Foo, "123"), equalTo(Foo.of(123)))
        assertThrows<LensFailure> { rawLens.ofOrLensFailure(Foo, "BAR") }
    }
}
