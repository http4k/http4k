package org.http4k.connect.openfga.model

import dev.forkhandles.values.ComparableValue
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Relation private constructor(value: String) : StringValue(value), ComparableValue<Relation, String> {
    companion object : NonBlankStringValueFactory<Relation>(::Relation)
}
