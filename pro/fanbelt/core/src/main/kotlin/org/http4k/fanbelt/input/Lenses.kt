/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.input

import org.http4k.lens.LensFailure

@PublishedApi
internal inline fun <T> lensOrNull(read: () -> T): T? = try {
    read()
} catch (_: LensFailure) {
    null
}
