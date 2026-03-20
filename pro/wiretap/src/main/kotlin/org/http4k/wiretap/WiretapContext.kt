/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import io.opentelemetry.api.OpenTelemetry
import org.http4k.core.HttpHandler

class WiretapContext(
    private val httpHandler: HttpHandler,
    private val oTelFn: (String) -> OpenTelemetry
) {
    fun http(): HttpHandler = httpHandler
    fun otel(serviceName: String = "http4k server"): OpenTelemetry = oTelFn(serviceName)
}
