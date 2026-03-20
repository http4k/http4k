/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import io.opentelemetry.api.OpenTelemetry
import org.http4k.core.HttpHandler
import java.time.Clock
import java.util.Random

/**
 *
 */
class Context(
    private val httpHandler: HttpHandler,
    private val clock: Clock,
    private val random: Random,
    private val oTelFn: (String) -> OpenTelemetry
) {
    fun clock() = clock
    fun http() = httpHandler
    fun random() = random
    fun otel(serviceName: String = "http4k server") = oTelFn(serviceName)
}
