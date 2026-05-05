package org.http4k.connect.amazon.sts.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.text
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.sts.STSAction
import org.http4k.connect.amazon.sts.model.CallerIdentity
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE

@Http4kConnectAction
class GetCallerIdentity : STSAction<CallerIdentity> {
    override fun toRequest() = Request(POST, uri())
        .with(CONTENT_TYPE of APPLICATION_FORM_URLENCODED)
        .form("Action", "GetCallerIdentity")
        .form("Version", "2011-06-15")

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(
                with(xmlDoc()) {
                    CallerIdentity(
                        UserId = text("UserId"),
                        Account = AwsAccount.of(text("Account")),
                        Arn = ARN.of(text("Arn"))
                    )
                }
            )

            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun uri() = Uri.of("")
}
