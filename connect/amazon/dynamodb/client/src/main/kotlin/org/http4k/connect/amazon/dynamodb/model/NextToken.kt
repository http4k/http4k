package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.AbstractComparableValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class NextToken private constructor(value: String) : AbstractComparableValue<NextToken, String>(value) {
    companion object : StringValueFactory<NextToken>(::NextToken, "([0-9a-f]{16})+".regex)
}
