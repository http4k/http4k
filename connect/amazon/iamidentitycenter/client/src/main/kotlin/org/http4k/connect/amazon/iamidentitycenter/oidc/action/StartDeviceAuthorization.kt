package org.http4k.connect.amazon.iamidentitycenter.oidc.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iamidentitycenter.IAMIdentityCenterMoshi
import org.http4k.connect.amazon.iamidentitycenter.OIDCAction
import org.http4k.connect.amazon.iamidentitycenter.model.ClientId
import org.http4k.connect.amazon.iamidentitycenter.model.ClientSecret
import org.http4k.connect.amazon.iamidentitycenter.model.DeviceCode
import org.http4k.connect.amazon.iamidentitycenter.model.UserCode
import org.http4k.connect.kClass
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class StartDeviceAuthorization(
    val clientId: ClientId,
    val clientSecret: ClientSecret,
    val startUrl: Uri
) : OIDCAction<AuthorizationStarted>(kClass()) {
    override fun toRequest() = Request(Method.POST, "device_authorization")
        .with(IAMIdentityCenterMoshi.autoBody<Any>().toLens() of this)
}

@JsonSerializable
data class AuthorizationStarted(
    val deviceCode: DeviceCode,
    val expiresIn: Long,
    val interval: Long,
    val userCode: UserCode,
    val verificationUri: Uri,
    val verificationUriComplete: Uri
)
