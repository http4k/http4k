package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class AttributeName private constructor(value: String) : StringValue(value), Comparable<AttributeName> {
    companion object : NonBlankStringValueFactory<AttributeName>(::AttributeName)

    override fun compareTo(other: AttributeName): Int = value.compareTo(other.value)
}
