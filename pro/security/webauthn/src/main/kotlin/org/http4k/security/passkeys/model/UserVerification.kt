/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.model

import com.squareup.moshi.Json

enum class UserVerification {
    @Json(name = "required") REQUIRED,
    @Json(name = "preferred") PREFERRED,
    @Json(name = "discouraged") DISCOURAGED
}
