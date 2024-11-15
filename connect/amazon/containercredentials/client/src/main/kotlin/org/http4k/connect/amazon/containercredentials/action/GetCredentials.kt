package org.http4k.connect.amazon.containercredentials.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.values.parseOrNull
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.containercredentials.ContainerCredentials
import org.http4k.connect.amazon.containercredentials.ContainerCredentialsAction
import org.http4k.connect.amazon.containercredentials.ContainerCredentialsMoshi
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.connect.amazon.core.model.Expiration
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class GetCredentials(private val uri: Uri) : ContainerCredentialsAction<Credentials> {
    override fun toRequest() = Request(GET, uri)

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(
                ContainerCredentialsMoshi.asA<GetCredentialsResponse>(bodyString()).asCredentials()
            )

            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class GetCredentialsResponse(
    val Token: SessionToken,
    val AccessKeyId: AccessKeyId,
    val SecretAccessKey: SecretAccessKey,
    val Expiration: Expiration,
    val RoleArn: String?
) {
    fun asCredentials() = Credentials(
        Token, AccessKeyId, SecretAccessKey, Expiration, RoleArn?.let { ARN.parseOrNull(it) }
        // RoleArn in response is not always the right shape https://kotlinlang.slack.com/archives/C5AL3AKUY/p1704811893680939
    )
}

fun ContainerCredentials.getCredentials(uri: Uri) = this(GetCredentials(uri))
