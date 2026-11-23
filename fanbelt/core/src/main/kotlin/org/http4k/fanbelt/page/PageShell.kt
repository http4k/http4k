package org.http4k.fanbelt.page

import org.http4k.core.Uri

data class PageShell(val base: Uri, val live: Uri, val regions: Map<String, String>, val signals: String) {
    val connect = "@get('$live', {retry: 'always', retryMaxCount: 1000})"
}
