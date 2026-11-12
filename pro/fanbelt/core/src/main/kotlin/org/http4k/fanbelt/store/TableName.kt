/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class TableName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<TableName>(::TableName, "[A-Za-z_][A-Za-z0-9_]*".regex)
}
