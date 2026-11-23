/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Uri

data class PageShell(val base: Uri, val live: Uri, val regions: Map<String, String>, val signals: String) {
    val connect = "@get('$live', {retry: 'always', retryMaxCount: 1000})"
}
