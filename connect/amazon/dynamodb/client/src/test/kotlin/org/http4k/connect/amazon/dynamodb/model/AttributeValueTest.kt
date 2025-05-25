package org.http4k.connect.amazon.dynamodb.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AttributeValueTest {

    @Test
    fun `int = decimal for N`() = runBlocking {
        val int = AttributeValue.Num(123)
        val decimal = AttributeValue.Num(BigDecimal("123.0"))

        assertThat(int, equalTo(decimal))
    }

    @Test
    fun `int = decimal in NS`() = runBlocking {
        val intSet = AttributeValue.NumSet(setOf(123))
        val decimalSet = AttributeValue.NumSet(setOf(BigDecimal("123.0")))

        assertThat(intSet, equalTo(decimalSet))
    }

    @Test
    fun `comparing numbers not done lexicographically`() = runBlocking {
        val higher = AttributeValue.Num(123)
        val lower = AttributeValue.Num(45)

        assertThat(higher, greaterThan(lower))
    }
}
