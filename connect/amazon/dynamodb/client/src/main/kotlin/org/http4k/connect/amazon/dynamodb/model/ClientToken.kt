package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.AbstractComparableValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex
import java.util.UUID


class ClientToken private constructor(value: String) : AbstractComparableValue<ClientToken, String>(value) {
    companion object : StringValueFactory<ClientToken>(::ClientToken, "^[^\\\$]+\$".regex) {
        fun random() = of(UUID.randomUUID().toString())
    }
}
