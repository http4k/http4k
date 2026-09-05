package org.http4k.connect.amazon.cognitoidentity.action

import org.http4k.aws.AwsCredentials
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognitoidentity.CognitoIdentityAction
import org.http4k.connect.amazon.cognitoidentity.model.IdentityId
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class GetCredentialsForIdentity(
    val IdentityId: IdentityId,
    val Logins: Map<String, String>? = null,
    val CustomRoleArn: ARN? = null,
) : CognitoIdentityAction<IdentityCredentials>(IdentityCredentials::class)

@JsonSerializable
data class IdentityCredentials(
    val IdentityId: IdentityId,
    val Credentials: TemporaryCredentials,
)

@JsonSerializable
data class TemporaryCredentials(
    val AccessKeyId: AccessKeyId,
    val SecretKey: SecretAccessKey,
    val SessionToken: SessionToken,
    val Expiration: Timestamp? = null,
) {
    fun asHttp4k() = AwsCredentials(AccessKeyId.value, SecretKey.value, SessionToken.value)
}
