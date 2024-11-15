package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.s3.S3Action
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri

private val defaultBucketRegion = Region.US_EAST_1

@Http4kConnectAction
data class CreateBucket(val bucketName: BucketName, val region: Region) : S3Action<Unit> {
    private fun uri() = Uri.of("/${bucketName}")

    override fun toRequest() = Request(PUT, uri()).let {
        if (region == defaultBucketRegion) it else it.body(
"""<?xml version="1.0" encoding="UTF-8"?>
<CreateBucketConfiguration xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
    <LocationConstraint>${region}</LocationConstraint>
</CreateBucketConfiguration>"""
)
    }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }
}
