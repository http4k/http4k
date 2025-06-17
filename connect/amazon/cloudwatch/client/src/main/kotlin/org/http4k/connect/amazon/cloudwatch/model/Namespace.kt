package org.http4k.connect.amazon.cloudwatch.model

import dev.forkhandles.values.AbstractComparableValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class Namespace private constructor(value: String) : AbstractComparableValue<Namespace, String>(value) {
    companion object : StringValueFactory<Namespace>(::Namespace, "[^:].*".regex)
}
