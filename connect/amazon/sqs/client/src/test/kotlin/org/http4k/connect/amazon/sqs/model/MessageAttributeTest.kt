package org.http4k.connect.amazon.sqs.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.http4k.connect.amazon.core.model.DataType
import org.http4k.connect.amazon.core.model.MessageFieldsDto
import org.http4k.connect.model.Base64Blob
import org.junit.jupiter.api.Test

class MessageAttributeTest {

    private fun roundTrip(
        sqs: MessageAttribute,
        dto: MessageFieldsDto
    ) {
        assertThat(sqs.toDto(), equalTo(dto))

        // sqs model is not a data class
        assertThat(dto.toSqs(sqs.name).name, equalTo(sqs.name))
        assertThat(dto.toSqs(sqs.name).value, equalTo(sqs.value))
        assertThat(dto.toSqs(sqs.name).dataType, equalTo(sqs.dataType))
    }

    @Test
    fun `round trip string value`() = roundTrip(
        MessageAttribute("foo", "bar", DataType.String),
        MessageFieldsDto(DataType.String, stringValue = "bar")
    )

    @Test
    fun `round trip string list value`() = roundTrip(
        MessageAttribute("foo", listOf("bar", "baz"), DataType.String),
        MessageFieldsDto(DataType.String, stringListValues = listOf("bar", "baz"))
    )

    @Test
    fun `round trip number value`() = roundTrip(
        MessageAttribute("foo", "1337", DataType.Number),
        MessageFieldsDto(DataType.Number, stringValue = "1337")
    )

    @Test
    fun `round trip number list value`() = roundTrip(
        MessageAttribute("foo", listOf("1337", "9001"), DataType.Number),
        MessageFieldsDto(DataType.Number, stringListValues = listOf("1337", "9001"))
    )

    @Test
    fun `round trip binary value`() = roundTrip(
        MessageAttribute("foo", Base64Blob.encode("bar")),
        MessageFieldsDto(DataType.Binary, binaryValue = "bar".base64Encode())
    )

    @Test
    fun `round trip binary list value`() = roundTrip(
        MessageAttribute("foo", listOf(Base64Blob.encode("foo"), Base64Blob.encode("bar"))),
        MessageFieldsDto(DataType.Binary, binaryListValues = listOf("foo".base64Encode(), "bar".base64Encode()))
    )
}
