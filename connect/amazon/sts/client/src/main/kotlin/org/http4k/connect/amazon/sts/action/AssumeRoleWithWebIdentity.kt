package org.http4k.connect.amazon.sts.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.connect.amazon.core.model.Expiration
import org.http4k.connect.amazon.core.model.RoleSessionName
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.core.model.WebIdentityToken
import org.http4k.connect.amazon.core.text
import org.http4k.connect.amazon.core.textOptional
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.sts.STSAction
import org.http4k.connect.amazon.sts.model.RoleId
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import java.time.Duration

@Http4kConnectAction
data class AssumeRoleWithWebIdentity(
    val RoleArn: ARN,
    val RoleSessionName: RoleSessionName,
    val WebIdentityToken: WebIdentityToken,
    val DurationSeconds: Duration? = null,
    val Policy: String? = null,
    val PolicyArns: List<ARN>? = null,
    val ProviderId: String? = null,
    val Tags: List<Tag>? = null
) : STSAction<AssumedRoleWithWebIdentityResponse> {

    override fun toRequest(): Request {
        val base = listOf(
            "Action" to "AssumeRoleWithWebIdentity",
            "RoleSessionName" to RoleSessionName.value,
            "RoleArn" to RoleArn.value,
            "WebIdentityToken" to WebIdentityToken.value,
            "Version" to "2011-06-15"
        )

        val policies = PolicyArns?.mapIndexed { index, next ->
            "PolicyArns.member.${index}.arn" to next.value
        }

        val tags = Tags?.flatMapIndexed { index, next ->
            listOf("Tags.member.${index}.Key" to next.Key, "Tags.member.${index}.Value" to next.Value)
        }

        val other = listOfNotNull(
            ProviderId?.let { "ProviderId" to it },
            Policy?.let { "Policy" to it },
            DurationSeconds?.let { "DurationSeconds" to it.seconds.toString() },
        )

        return listOfNotNull(base, policies, tags, other)
            .flatten().fold(
                Request(POST, uri())
                    .with(CONTENT_TYPE of APPLICATION_FORM_URLENCODED)
            ) { acc, it ->
                acc.form(it.first, it.second)
            }
    }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(AssumedRoleWithWebIdentityResponse.from(this))
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun uri() = Uri.of("")
}

data class AssumedRoleWithWebIdentityResponse(
    override val AssumedRoleId: RoleId,
    override val Credentials: Credentials,
    val SubjectFromWebIdentityToken: String,
    val Audience: String,
    val SourceIdentity: String?,
    val Provider: String
) : AssumedRole {
    companion object {
        fun from(response: Response) =
            with(response.xmlDoc()) {
                AssumedRoleWithWebIdentityResponse(
                    RoleId.of(text("AssumedRoleId")),
                    Credentials(
                        SessionToken.of(text("SessionToken")),
                        AccessKeyId.of(text("AccessKeyId")),
                        SecretAccessKey.of(text("SecretAccessKey")),
                        Expiration.parse(text("Expiration")),
                        ARN.of(text("Arn"))
                    ),
                    text("SubjectFromWebIdentityToken"),
                    text("Audience"),
                    textOptional("SourceIdentity"),
                    text("Provider")
                )
            }
    }
}

