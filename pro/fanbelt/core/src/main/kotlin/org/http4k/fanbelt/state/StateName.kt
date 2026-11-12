/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.state

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class StateName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<StateName>(::StateName, "[a-z][A-Za-z0-9-]{0,60}".regex)
}
