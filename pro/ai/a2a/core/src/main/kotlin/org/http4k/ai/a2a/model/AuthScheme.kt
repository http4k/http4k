/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class AuthScheme private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<AuthScheme>(::AuthScheme) {
        val BEARER = of("bearer")
        val BASIC = of("basic")
        val API_KEY = of("apiKey")
        val OAUTH2 = of("oauth2")
    }
}
