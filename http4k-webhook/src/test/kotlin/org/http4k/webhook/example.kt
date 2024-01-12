package org.http4k.webhook

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.SignWebhookPayload
import org.http4k.filter.VerifyWebhookSignature

fun main() {
    val signingSecret = HmacSha256SigningSecret.encode("foobarfoobarfoobarfoobarfoobarfoobar")
    val app = ClientFilters.SignWebhookPayload(HmacSha256.Signer(signingSecret))
        .then(ClientFilters.VerifyWebhookSignature(HmacSha256.Verifier(signingSecret)))
        .then {
            Response(OK)
        }

    app(Request(Method.GET, ""))
}
