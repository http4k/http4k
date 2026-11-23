package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.sameInstance
import org.junit.jupiter.api.Test

object Robot

data class Acted(val actor: Any, val by: Robot)

class ObjectsByNameAdapterTest {
    private val json = ConfigurableMoshi(standardConfig().done().add(ObjectsByNameAdapter))

    @Test
    fun `an object is written as its name, wherever it appears`() {
        assertThat(json.asFormatString(Acted(Robot, Robot)), equalTo("""{"actor":"Robot","by":"Robot"}"""))
    }

    @Test
    fun `an object reads back as the same instance`() {
        assertThat(json.asA<Robot>("\"Robot\""), sameInstance(Robot))
    }
}
