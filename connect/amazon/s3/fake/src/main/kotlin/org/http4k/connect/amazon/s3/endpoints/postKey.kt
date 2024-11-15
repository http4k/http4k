package org.http4k.connect.amazon.s3.endpoints

import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.model.RfcTimestamp
import org.http4k.connect.amazon.core.sequenceOfNodes
import org.http4k.connect.amazon.core.text
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.s3.BucketKeyContent
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.amazon.s3.replaceHeader
import org.http4k.connect.amazon.s3.requiresRestore
import org.http4k.connect.amazon.s3.storageClass
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.time.Clock
import java.time.Duration

fun bucketPostKey(buckets: Storage<Unit>,  bucketContent: Storage<BucketKeyContent>, clock: Clock) =
    "/{bucketKey:.+}" bind POST to routes(
        queryPresent("restore") bind { request ->
            restoreObject(
                buckets,
                bucketContent,
                BucketName.of(request.subdomain(buckets)),
                BucketKey.of(request.path("bucketKey")!!),
                request,
                clock
            )
        }
    )

fun pathBasedBucketPostKey(buckets: Storage<Unit>,  bucketContent: Storage<BucketKeyContent>, clock: Clock) =
    "/{bucketName}/{bucketKey:.+}" bind POST to routes(
        queryPresent("restore") bind { request ->
            restoreObject(
                buckets,
                bucketContent,
                BucketName.of(request.path("bucketName")!!),
                BucketKey.of(request.path("bucketKey")!!),
                request,
                clock
            )
        }
    )


private fun restoreObject(
    buckets: Storage<Unit>,
    bucketContent: Storage<BucketKeyContent>,
    bucket: BucketName,
    bucketKey: BucketKey,
    request: Request,
    clock: Clock
): Response {
    val doc = request.body.xmlDoc().getElementsByTagName("RestoreRequest").sequenceOfNodes().first()

    val days = doc.firstChild("Days")!!.text().toLong()
    val expires = clock.instant() + Duration.ofDays(days)

    if (buckets[bucket.value] == null) return invalidBucketNameResponse()
    val obj = bucketContent["$bucket-$bucketKey"] ?: return invalidBucketKeyResponse()
    if (!obj.storageClass().requiresRestore()) return invalidObjectStateResponse()

    // instantly restore the object, regardless of restore tier
    bucketContent["$bucket-$bucketKey"] = obj.replaceHeader(
        "x-amz-restore",
        "ongoing-request=\"false\", expiry-date=\"${RfcTimestamp.of(expires)}\""
    )

    return Response(OK)
}
