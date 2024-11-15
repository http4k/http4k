package org.http4k.connect.amazon.iamidentitycenter.endpoints

import org.http4k.connect.amazon.iamidentitycenter.IAMIdentityCenterMoshi
import org.http4k.connect.amazon.iamidentitycenter.IAMIdentityCenterMoshi.auto
import org.http4k.connect.amazon.iamidentitycenter.model.ClientId
import org.http4k.connect.amazon.iamidentitycenter.model.ClientSecret
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.RegisterClient
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.RegisteredClient
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.routing.bind
import java.time.Clock
import org.http4k.connect.model.Timestamp

fun registerClient(clock: Clock) = "/client/register" bind Method.POST to { req: Request ->
    val request = Body.auto<RegisterClient>().toLens()(req)
    Response(Status.OK)
        .with(
            IAMIdentityCenterMoshi.autoBody<Any>().toLens() of RegisteredClient(
                ClientId.of(request.clientName.value),
                ClientSecret.of(request.clientName.value),
                Timestamp.of(clock.instant().plusSeconds(3600)),
                Timestamp.of(clock.instant().plusSeconds(3600)),
                null,
                null,
            )
        )
}
