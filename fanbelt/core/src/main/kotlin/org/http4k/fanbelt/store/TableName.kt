package org.http4k.fanbelt.store

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

/**
 * The name of a table. The app's migrations create it; Fanbelt never creates schema.
 */
class TableName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<TableName>(::TableName, "[A-Za-z_][A-Za-z0-9_]*".regex)
}
