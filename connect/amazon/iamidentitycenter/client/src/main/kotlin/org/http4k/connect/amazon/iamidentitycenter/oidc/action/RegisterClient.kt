package org.http4k.connect.amazon.iamidentitycenter.oidc.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iamidentitycenter.IAMIdentityCenterMoshi
import org.http4k.connect.amazon.iamidentitycenter.OIDCAction
import org.http4k.connect.amazon.iamidentitycenter.model.ClientId
import org.http4k.connect.amazon.iamidentitycenter.model.ClientName
import org.http4k.connect.amazon.iamidentitycenter.model.ClientSecret
import org.http4k.connect.kClass
import org.http4k.connect.model.Timestamp
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class RegisterClient(
    val clientName: ClientName,
    val scopes: List<String>? = null
) : OIDCAction<RegisteredClient>(kClass()) {

    override fun toRequest() = Request(Method.POST, "/client/register")
        .with(
            IAMIdentityCenterMoshi.autoBody<Any>().toLens() of mapOf(
                "clientName" to clientName,
                "scopes" to scopes,
                "clientType" to "public"
            )
        )
}

@JsonSerializable
data class RegisteredClient(
    val clientId: ClientId,
    val clientSecret: ClientSecret,
    val clientIdIssuedAt: Timestamp,
    val clientSecretExpiresAt: Timestamp,
    val tokenEndpoint: Uri?,
    val authorizationEndpoint: Uri?,
)
