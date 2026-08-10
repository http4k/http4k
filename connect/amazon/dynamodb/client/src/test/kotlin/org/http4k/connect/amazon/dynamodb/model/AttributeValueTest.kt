package org.http4k.connect.amazon.dynamodb.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Base64
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Base64Set
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Bool
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.List
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Map
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Null
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Num
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.NumSet
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Str
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.StrSet
import org.http4k.connect.model.Base64Blob
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AttributeValueTest {

    @Test
    fun `int = decimal for N`() {
        val int = Num(123)
        val decimal = Num(BigDecimal("123.0"))

        assertThat(int, equalTo(decimal))
    }

    @Test
    fun `int = decimal in NS`() {
        val intSet = NumSet(setOf(123))
        val decimalSet = NumSet(setOf(BigDecimal("123.0")))

        assertThat(intSet, equalTo(decimalSet))
    }

    @Test
    fun `comparing numbers not done lexicographically`() {
        val higher = Num(123)
        val lower = Num(45)

        assertThat(higher, greaterThan(lower))
    }

    @Test
    fun `each type is only equal to its own kind of value`() {
        val values = listOf(
            Base64(blob), Bool(true), Base64Set(setOf(blob)), List(listOf(Str("a"))),
            Map(mapOf(AttributeName.of("a") to Str("b"))), Num(1), NumSet(setOf(1)),
            Null(), Str("a"), StrSet(setOf("a"))
        )

        values.forEach { value ->
            assertThat("$value equals itself", value, equalTo(value))
            values.filterNot { it === value }.forEach { other ->
                assertThat("$value should not equal $other", value == other, equalTo(false))
            }
        }
    }

    @Test
    fun `is never equal to a non-AttributeValue`() {
        assertThat(Str("a").equals("a"), equalTo(false))
    }

    @Test
    fun `equal values have equal hash codes, and booleans do not collide`() {
        assertThat(Str("a").hashCode(), equalTo(Str("a").hashCode()))
        assertThat(Base64(blob).hashCode(), equalTo(Base64(blob).hashCode()))
        assertThat(List(listOf(Str("a"))).hashCode(), equalTo(List(listOf(Str("a"))).hashCode()))
        assertThat(Bool(true).hashCode() == Bool(false).hashCode(), equalTo(false))
    }

    @Test
    fun `renders only the populated field`() {
        assertThat(Str("a").toString(), equalTo("AttributeValue(S=a)"))
        assertThat(Null().toString(), equalTo("AttributeValue(NULL=true)"))
        assertThat(List(listOf(Str("a"))).toString(), equalTo("AttributeValue(L=[AttributeValue(S=a)])"))
    }

    @Test
    fun `strings and blobs compare by content, mismatched types do not order`() {
        assertThat(Str("b"), greaterThan(Str("a")))
        assertThat(Base64(Base64Blob.encode("b")), greaterThan(Base64(Base64Blob.encode("a"))))
        assertThat(Str("a").compareTo(Num(1)), equalTo(0))
    }

    @Test
    fun `adding combines numbers and collections, leaving mismatched types untouched`() {
        assertThat(Num(1) + Num(2), equalTo(Num(3)))
        assertThat(NumSet(setOf(1)) + NumSet(setOf(2)), equalTo(NumSet(setOf(1, 2))))
        assertThat(StrSet(setOf("a")) + StrSet(setOf("b")), equalTo(StrSet(setOf("a", "b"))))
        assertThat(List(listOf(Str("a"))) + List(listOf(Str("b"))), equalTo(List(listOf(Str("a"), Str("b")))))
        assertThat(Base64Set(setOf(blob)) + Base64Set(setOf(otherBlob)), equalTo(Base64Set(setOf(blob, otherBlob))))
        assertThat(Num(1) + Str("a"), equalTo(Num(1)))
    }

    @Test
    fun `subtracting removes members, leaving mismatched types untouched`() {
        assertThat(Num(3) - Num(2), equalTo(Num(1)))
        assertThat(NumSet(setOf(1, 2)) - NumSet(setOf(2)), equalTo(NumSet(setOf(1))))
        assertThat(StrSet(setOf("a", "b")) - StrSet(setOf("b")), equalTo(StrSet(setOf("a"))))
        assertThat(Base64Set(setOf(blob, otherBlob)) - Base64Set(setOf(otherBlob)), equalTo(Base64Set(setOf(blob))))
        assertThat(Str("a") - Num(1), equalTo(Str("a")))
    }

    @Test
    fun `setting a list index replaces or appends`() {
        val list = List(listOf(Str("a"), Str("b")))

        assertThat(list.with(0, Str("z")), equalTo(List(listOf(Str("z"), Str("b")))))
        assertThat(list.with(99, Str("z")), equalTo(List(listOf(Str("a"), Str("b"), Str("z")))))
        assertThat(Str("a").with(0, Str("z")), equalTo(Str("a")))
    }

    @Test
    fun `deleting removes the item at an index`() {
        val list = List(listOf(Str("a"), Str("b"), Str("c")))

        assertThat(list.delete(1), equalTo(List(listOf(Str("a"), Str("c")))))
        assertThat(Str("a").delete(0), equalTo(Str("a")))
    }

    private val blob = Base64Blob.encode("hello")
    private val otherBlob = Base64Blob.encode("goodbye")
}
