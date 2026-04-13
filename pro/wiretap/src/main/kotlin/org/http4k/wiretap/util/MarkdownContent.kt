/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.util

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory

class MarkdownContent private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<MarkdownContent>(::MarkdownContent) {
        val empty = of("")
    }
}
