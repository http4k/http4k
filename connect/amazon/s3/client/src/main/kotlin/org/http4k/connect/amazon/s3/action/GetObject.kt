package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.s3.S3BucketAction
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import java.io.InputStream

@Http4kConnectAction
data class GetObject(val key: BucketKey) : S3BucketAction<InputStream?> {
    override fun toRequest() = Request(GET, uri())

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(body.stream)
            status == NOT_FOUND -> Success(null)
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun uri() = Uri.of("/${key}")
}
