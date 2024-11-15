package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.s3.S3BucketAction
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.HEAD
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri

@Http4kConnectAction
data class HeadBucket(val expectedBucketOwner: String? = null) : S3BucketAction<Unit?> {

    override fun toRequest() = Request(HEAD, uri()).run {
        expectedBucketOwner?.let { header("x-amz-expected-bucket-owner", it) } ?: this
    }

    private fun uri() = Uri.of("")

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            status == NOT_FOUND -> Success(null)
            else -> Failure(asRemoteFailure(this))
        }
    }
}
