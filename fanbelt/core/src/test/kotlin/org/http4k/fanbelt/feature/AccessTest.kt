package org.http4k.fanbelt.feature

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.input.FieldName
import org.junit.jupiter.api.Test

class AccessTest {
    private val refusals: List<Access<String>> = listOf(Hidden, Denied, Rejected(FieldName.of("room"), "That room has gone"))

    @Test
    fun `map changes what an allowed answer found, and leaves a refusal as it is`() {
        assertThat(Allowed("kitchen").map { it.length - 4 }, equalTo(Allowed(3)))
        refusals.forEach { assertThat(it.map { found -> found.length }, equalTo(it)) }
    }

    @Test
    fun `flatMap carries on from an allowed answer, which may refuse, and leaves a refusal as it is`() {
        assertThat(Allowed("kitchen").flatMap { Allowed(it.length) }, equalTo(Allowed(7)))
        assertThat(Allowed("kitchen").flatMap { Denied }, equalTo(Denied))
        refusals.forEach { assertThat(it.flatMap { found -> Allowed(found.length) }, equalTo(it)) }
    }

    @Test
    fun `what an allowed answer found can be read, or nothing if it refused`() {
        assertThat(Allowed("found").orNull(), equalTo("found"))
        refusals.forEach { assertThat(it.orNull(), absent()) }
    }
}
