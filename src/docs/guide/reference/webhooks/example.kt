package guide.reference.webhooks

import org.http4k.client.JavaHttpClient
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.SignWebhookPayload
import org.http4k.filter.VerifyWebhookSignature
import org.http4k.format.Jackson.auto
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.webhook.EventType
import org.http4k.webhook.WebhookPayload
import org.http4k.webhook.WebhookTimestamp
import org.http4k.webhook.signing.HmacSha256
import org.http4k.webhook.signing.HmacSha256SigningSecret
import java.time.Instant

data class MyEventType(val index: Int)

fun main() {
    val lens = Body.auto<WebhookPayload<MyEventType>>().toLens()

    val signingSecret = HmacSha256SigningSecret.encode("this_is_my_super_secret")

    val sendingClient = ClientFilters.SignWebhookPayload(HmacSha256.Signer(signingSecret))
        .then(JavaHttpClient())

    val webhookReceiver =
        ClientFilters.VerifyWebhookSignature(HmacSha256.Verifier(signingSecret))
            .then { req: Request ->
                println(lens(req))
                Response(OK)
            }

    val server = webhookReceiver.asServer(SunHttp(0)).start()

    // create the webhook event wrapper with event type, timestamp
    val webhookPayload = WebhookPayload(
        EventType.of("foo.bar"),
        WebhookTimestamp.of(Instant.EPOCH),
        MyEventType(123)
    )

    val eventRequest =
        Request(POST, Uri.of("http://localhost:${server.port()}")).with(lens of webhookPayload)
    sendingClient(eventRequest)
}
