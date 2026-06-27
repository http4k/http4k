/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.model

import org.http4k.core.Uri

data class RelyingParty(val id: String, val name: String, val origins: Set<Uri>) {
    constructor(id: String, name: String, origin: Uri) : this(id, name, setOf(origin))
}
