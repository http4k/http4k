package org.http4k.connect.amazon.iamidentitycenter.oidc.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iamidentitycenter.IAMIdentityCenterMoshi
import org.http4k.connect.amazon.iamidentitycenter.OIDCAction
import org.http4k.connect.amazon.iamidentitycenter.model.AccessToken
import org.http4k.connect.amazon.iamidentitycenter.model.AuthCode
import org.http4k.connect.amazon.iamidentitycenter.model.ClientId
import org.http4k.connect.amazon.iamidentitycenter.model.ClientSecret
import org.http4k.connect.amazon.iamidentitycenter.model.DeviceCode
import org.http4k.connect.amazon.iamidentitycenter.model.GrantType
import org.http4k.connect.amazon.iamidentitycenter.model.IdToken
import org.http4k.connect.amazon.iamidentitycenter.model.PKCECodeVerifier
import org.http4k.connect.amazon.iamidentitycenter.model.RefreshToken
import org.http4k.connect.amazon.iamidentitycenter.model.SessionId
import org.http4k.connect.kClass
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateToken(
    val clientId: ClientId,
    val clientSecret: ClientSecret,
    val grantType: GrantType,
    val deviceCode: DeviceCode?,
    val redirectUri: Uri?,
    val codeVerifier: PKCECodeVerifier?,
    val code: AuthCode?,
) : OIDCAction<DeviceToken>(kClass()) {
    override fun toRequest() = Request(Method.POST, "token")
        .with(
            IAMIdentityCenterMoshi.autoBody<Any>().toLens() of mapOf(
                "clientId" to clientId,
                "clientSecret" to clientSecret,
                "grantType" to grantType.wireValue,
                "deviceCode" to deviceCode,
                "redirectUri" to redirectUri,
                "codeVerifier" to codeVerifier,
                "code" to code
            )
        )
}

@JsonSerializable
data class DeviceToken(
    val accessToken: AccessToken,
    val expiresIn: Long,
    val idToken: IdToken?,
    val refreshToken: RefreshToken?,
    val aws_sso_app_session_id: SessionId?,
    val originSessionId: SessionId?,
    val issuedTokenType: String?,
    val tokenType: String,
)
