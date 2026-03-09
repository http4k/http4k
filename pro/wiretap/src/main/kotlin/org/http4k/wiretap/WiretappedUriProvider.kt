/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import io.opentelemetry.api.OpenTelemetry
import org.http4k.core.HttpHandler
import org.http4k.core.Uri

/**
 * A function that returns the URI of an app to connect to a Wiretap instance.
 * For locally Wiretapped apps, use the passed HTTP client and OpenTelemetry instance to record
 * HTTP transactions and OTel traces to the local store.
 */
fun interface WiretappedUriProvider {
    operator fun invoke(http: HttpHandler, oTel: OpenTelemetry): Uri
}
