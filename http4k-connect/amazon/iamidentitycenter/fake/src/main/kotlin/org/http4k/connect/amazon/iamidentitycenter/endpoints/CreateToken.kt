package org.http4k.connect.amazon.iamidentitycenter.endpoints

import org.http4k.connect.amazon.iamidentitycenter.IAMIdentityCenterMoshi
import org.http4k.connect.amazon.iamidentitycenter.IAMIdentityCenterMoshi.auto
import org.http4k.connect.amazon.iamidentitycenter.model.AccessToken
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.CreateToken
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.DeviceToken
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.routing.bind

fun createToken() = "/token" bind Method.POST to { req: Request ->
    val request = Body.auto<CreateToken>().toLens()(req)
    Response(Status.OK)
        .with(
            IAMIdentityCenterMoshi.autoBody<Any>().toLens() of DeviceToken(
                AccessToken.of("AccessToken-" + request.clientId + request.clientSecret + request.deviceCode),
                3600,
                null,
                null,
                null,
                null,
                null,
                "Bearer"
            )
        )
}
