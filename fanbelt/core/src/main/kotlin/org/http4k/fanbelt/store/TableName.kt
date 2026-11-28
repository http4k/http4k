package org.http4k.fanbelt.store

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

/**
 * The name of a table, which the app's migrations create.
 */
class TableName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<TableName>(::TableName, "[A-Za-z_][A-Za-z0-9_]*".regex)
}
