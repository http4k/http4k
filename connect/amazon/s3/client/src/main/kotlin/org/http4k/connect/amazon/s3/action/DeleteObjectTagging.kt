package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.s3.S3BucketAction
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.DELETE
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri

@Http4kConnectAction
class DeleteObjectTagging(val key: BucketKey): S3BucketAction<Unit> {

    override fun toRequest() =  Request(DELETE, Uri.of("/$key?tagging"))

    override fun toResult(response: Response) = when {
        response.status.successful -> Success(Unit)
        else -> Failure(asRemoteFailure(response))
    }
}
