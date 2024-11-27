package org.http4k.connect.amazon.dynamodb.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.InstantValue
import dev.forkhandles.values.InstantValueFactory
import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.LongValue
import dev.forkhandles.values.LongValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Null
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Num
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Str
import org.http4k.lens.LensFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class AttributeTest {

    @Test
    fun `can create value from attributes`() {
        assertThat(
            Attribute.value(MyLongType).required("name").asValue(MyLongType.of(1)),
            equalTo(Num(1))
        )
        assertThat(Attribute.value(MyIntType).required("name").asValue(MyIntType.of(1)), equalTo(Num(1)))
        assertThat(
            Attribute.value(MyStringType).required("name").asValue(MyStringType.of("foo")),
            equalTo(Str("foo"))
        )
        assertThat(
            Attribute.value(MyInstantType).required("name").asValue(MyInstantType.of(Instant.EPOCH)),
            equalTo(Str("1970-01-01T00:00:00Z"))
        )
        assertThat(
            Attribute.uuid().value(MyUUIDType).required("name").asValue(MyUUIDType.of(UUID(0, 0))),
            equalTo(Str("00000000-0000-0000-0000-000000000000"))
        )
    }

    @Test
    fun `defaulted falls back to another lens`() {
        val fallback: Attribute<UUID> = Attribute.string().map(UUID::fromString, UUID::toString).required("fallback")
        val primary = Attribute.uuid().defaulted("primary", fallback)

        assertThat(
            primary(Item(fallback of UUID(0, 0))),
            equalTo(UUID(0, 0))
        )
    }

    @Test
    fun `may ignore null values in optional attributes`() {
        // given
        val optionalWithNull = Attribute.string().optional("withNull")
        val optionalWithoutNull = Attribute.string().optional("withoutNull", ignoreNull = true)

        // when
        val itemWithNull = Item(optionalWithNull of null)
        val itemWithoutNull = Item(optionalWithoutNull of null)
        val itemWithValue = Item(optionalWithoutNull of "something")

        // then
        assertThat(itemWithNull, equalTo(mapOf(optionalWithNull.name to Null())))
        assertThat(itemWithoutNull, equalTo(emptyMap()))
        assertThat(itemWithValue, equalTo(mapOf(optionalWithoutNull.name to Str("something"))))

        assertThat(optionalWithNull(itemWithNull), equalTo(null))
        assertThat(optionalWithoutNull(itemWithoutNull), equalTo(null))
        assertThat(optionalWithoutNull(itemWithValue), equalTo("something"))
    }

    @Test
    fun `convert optional Attribute to required Attribute`() {
        // given
        val givenOptionalInt = Attribute.int().optional("intValue")
        val givenRequiredInt = Attribute.int().required("intValue")
        val givenEmptyItem = Item()
        val givenIntItem = Item(givenRequiredInt of 42)
        val givenFailure = assertThrows<LensFailure> {
            givenRequiredInt(givenEmptyItem)
        }

        // when
        val actualRequiredInt = givenOptionalInt.asRequired()

        // then name and dataType should be taken from the given optional attribute
        assertThat(actualRequiredInt.name, equalTo(givenOptionalInt.name))
        assertThat(actualRequiredInt.dataType, equalTo(givenOptionalInt.dataType))

        // and behaviour should be the same as for the required attribute
        assertThat(actualRequiredInt(givenIntItem), equalTo(givenRequiredInt(givenIntItem)))
        assertThat(Item(actualRequiredInt of 23), equalTo(Item(givenRequiredInt of 23)))

        assertThrows<LensFailure> {
            actualRequiredInt(givenEmptyItem)
        }.apply {
            assertThat(failures, equalTo(givenFailure.failures))
            assertThat(target, equalTo(givenFailure.target))
        }
    }
}

class MyStringType(value: String) : StringValue(value) {
    companion object : StringValueFactory<MyStringType>(::MyStringType)
}

class MyUUIDType(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<MyUUIDType>(::MyUUIDType)
}

class MyIntType(value: Int) : IntValue(value) {
    companion object : IntValueFactory<MyIntType>(::MyIntType)
}

class MyLongType(value: Long) : LongValue(value) {
    companion object : LongValueFactory<MyLongType>(::MyLongType)
}

class MyInstantType(value: Instant) : InstantValue(value) {
    companion object : InstantValueFactory<MyInstantType>(::MyInstantType)
}
