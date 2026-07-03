package org.http4k.filter

import org.http4k.connect.github.GitHubToken
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.lens.Header
import org.http4k.lens.X_HUB_SIGNATURE_256
import org.http4k.security.Sha256.hmac
import org.http4k.security.secureEquals
import java.util.Locale

fun ServerFilters.VerifyGitHubSignatureSha256(token: () -> GitHubToken) = Filter { next ->
    {
        val expected = hmac(token().value.toByteArray(), it.bodyString()).toHexString()
        if (secureEquals(Header.X_HUB_SIGNATURE_256(it), expected)) {
            next(it)
        } else {
            Response(UNAUTHORIZED)
        }
    }
}

private fun ByteArray.toHexString() = joinToString("") { String.format(Locale.ROOT, "%02x", (it.toInt() and 0xFF)) }
