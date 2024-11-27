package org.http4k.filter

import org.http4k.connect.github.GitHubToken
import org.http4k.core.Filter
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.X_HUB_SIGNATURE_256
import org.http4k.security.HmacSha256.hmacSHA256


fun ClientFilters.SignGitHubWebhookSha256(token: () -> GitHubToken) = Filter { next ->
    {
        next(
            it.with(
                Header.X_HUB_SIGNATURE_256 of hmacSHA256(token().value.toByteArray(), it.bodyString())
                    .toHexString()
            )
        )
    }
}

private fun ByteArray.toHexString() = joinToString("") { String.format("%02x", (it.toInt() and 0xFF)) }
