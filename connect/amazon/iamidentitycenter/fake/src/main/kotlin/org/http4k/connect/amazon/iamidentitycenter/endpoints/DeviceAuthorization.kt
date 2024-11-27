package org.http4k.connect.amazon.iamidentitycenter.endpoints

import org.http4k.connect.amazon.iamidentitycenter.IAMIdentityCenterMoshi
import org.http4k.connect.amazon.iamidentitycenter.IAMIdentityCenterMoshi.auto
import org.http4k.connect.amazon.iamidentitycenter.model.DeviceCode
import org.http4k.connect.amazon.iamidentitycenter.model.UserCode
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.AuthorizationStarted
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.StartDeviceAuthorization
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.core.with
import org.http4k.routing.bind

fun deviceAuthorization() = "/device_authorization" bind Method.POST to { req: Request ->
    val request = Body.auto<StartDeviceAuthorization>().toLens()(req)
    Response(Status.OK)
        .with(
            IAMIdentityCenterMoshi.autoBody<Any>().toLens() of AuthorizationStarted(
                DeviceCode.of("DEVICE-" + request.clientId + request.clientSecret + request.startUrl),
                3600,
                1000,
                UserCode.of("USER-" + request.clientId + request.clientSecret + request.startUrl),
                Uri.of("https://device.sso.ldn-north-1.amazonaws.com/"),
                Uri.of("https://device.sso.ldn-north-1.amazonaws.com/").query("user_code", "HTTP-4KOK")
            )
        )
}

