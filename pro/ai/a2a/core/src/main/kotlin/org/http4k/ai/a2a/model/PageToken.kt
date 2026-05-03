/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory

class PageToken private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<PageToken>(::PageToken) {
        val END = of("")
    }
}
