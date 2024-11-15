package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.s3.S3BucketAction
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.RestoreTier
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri

@Http4kConnectAction
data class RestoreObject(
    val key: BucketKey,
    val days: Int,
    val description: String? = null,
    val tier: RestoreTier? = null
) : S3BucketAction<Unit> {
    override fun toRequest() = Request(POST, uri()).body(
"""<?xml version="1.0" encoding="UTF-8"?>
<RestoreRequest xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
  <Days>$days</Days>
  ${ if (tier != null) "<GlacierJobParameters><Tier>$tier</Tier></GlacierJobParameters>" else ""}
</RestoreRequest>"""
)

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun uri() = Uri.of("/$key?restore")
}
