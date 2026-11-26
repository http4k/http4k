package org.http4k.fanbelt.page

import org.http4k.core.Uri
import org.http4k.fanbelt.tenancy.TenantId

data class PageShell(val tenant: TenantId, val base: Uri, val live: Uri, val regions: Map<String, String>, val signals: String) {
    val connect = "@get('$live', {retry: 'always', retryMaxCount: 1000})"
}
