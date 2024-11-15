package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex
import org.http4k.connect.amazon.core.model.ResourceId

class TableName private constructor(value: String) : ResourceId(value) {
    companion object : StringValueFactory<TableName>(::TableName, "[a-zA-Z0-9_.-]+".regex)
}
