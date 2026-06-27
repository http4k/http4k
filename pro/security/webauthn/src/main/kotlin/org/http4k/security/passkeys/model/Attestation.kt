/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.model

import com.squareup.moshi.Json

enum class Attestation {
    @Json(name = "none") NONE,
    @Json(name = "direct") DIRECT,
    @Json(name = "indirect") INDIRECT,
    @Json(name = "enterprise") ENTERPRISE
}
