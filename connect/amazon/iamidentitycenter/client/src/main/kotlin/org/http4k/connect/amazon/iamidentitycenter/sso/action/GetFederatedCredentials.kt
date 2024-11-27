package org.http4k.connect.amazon.iamidentitycenter.sso.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.connect.amazon.iamidentitycenter.SSOAction
import org.http4k.connect.amazon.iamidentitycenter.model.AccessToken
import org.http4k.connect.amazon.iamidentitycenter.model.RoleName
import org.http4k.connect.kClass
import org.http4k.connect.model.Timestamp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class GetFederatedCredentials(
    val account: AwsAccount,
    val roleName: RoleName,
    val accessToken: AccessToken,
) : SSOAction<FederatedCredentials>(kClass()) {
    override fun toRequest() = Request(GET, "/federation/credentials")
        .header("x-amz-sso_bearer_token", accessToken.value)
        .query("role_name", roleName.value)
        .query("account_id", account.value)
}

@JsonSerializable
data class RoleCredentials(
    val accessKeyId: AccessKeyId,
    val secretAccessKey: SecretAccessKey,
    val sessionToken: SessionToken,
    val expiration: Timestamp
)

@JsonSerializable
data class FederatedCredentials(
    val roleCredentials: RoleCredentials
)
