package org.http4k.connect.amazon.evidently.model

import dev.forkhandles.values.AbstractComparableValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.Value
import dev.forkhandles.values.length

class EntityId private constructor(value: String) : AbstractComparableValue<Value<String>, String>(value) {
    companion object : StringValueFactory<EntityId>(::EntityId, (1..512).length)
}
