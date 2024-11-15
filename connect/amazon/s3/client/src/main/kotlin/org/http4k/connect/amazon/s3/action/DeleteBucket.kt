package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.s3.S3BucketAction
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.DELETE
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri

@Http4kConnectAction
class DeleteBucket : S3BucketAction<Unit?> {

    override fun toRequest() = Request(DELETE, uri())

    private fun uri() = Uri.of("")

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            status == NOT_FOUND -> Success(null)
            else -> Failure(asRemoteFailure(this))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode() = javaClass.hashCode()
}
