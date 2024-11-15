package org.http4k.connect.amazon.dynamodb.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.InstantValue
import dev.forkhandles.values.InstantValueFactory
import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.Instant.EPOCH

class StringType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<StringType>(::StringType)
}

class InstantType private constructor(value: Instant) : InstantValue(value) {
    companion object : InstantValueFactory<InstantType>(::InstantType)
}

class IntType private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<IntType>(::IntType)
}

class ExtensionsKtTest {

    @Test
    fun `int value list`() {
        val lens = Attribute.list(IntType).required("foo")
        val input = listOf(IntType.of(123))
        val target = Item().with(lens of input)
        assertThat(lens(target), equalTo(input))
    }

    @Test
    fun `non-string value list`() {
        val lens = Attribute.list(InstantType).required("foo")
        val input = listOf(InstantType.of(Instant.now()))
        val target = Item().with(lens of input)
        assertThat(lens(target), equalTo(input))
    }

    @Test
    fun `non-string value set`() {
        val lens = Attribute.set(InstantType).required("foo")
        val input = setOf(InstantType.of(EPOCH), InstantType.of(Instant.now()))
        val target = Item().with(lens of input)
        assertThat(lens(target), equalTo(input))
    }
}
