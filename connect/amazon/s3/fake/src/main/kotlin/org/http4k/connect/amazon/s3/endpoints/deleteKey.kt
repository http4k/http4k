package org.http4k.connect.amazon.s3.endpoints

import org.http4k.connect.amazon.s3.BucketKeyContent
import org.http4k.connect.storage.Storage
import org.http4k.core.Headers
import org.http4k.core.Method.DELETE
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.time.Clock

internal fun bucketDeleteKey(buckets: Storage<Unit>, bucketContent: Storage<BucketKeyContent>, clock: Clock) =
    "/{bucketKey:.+}" bind DELETE to routes(
        queryPresent("tagging") bind {
            deleteObjectTagging(it.subdomain(buckets), buckets, bucketContent, keyFor(it), it.headers, clock)
        },
        otherwise bind { req ->
            deleteObject(req.subdomain(buckets), buckets, bucketContent, keyFor(req))
        }
    )

internal fun pathBasedBucketDeleteKey(buckets: Storage<Unit>, bucketContent: Storage<BucketKeyContent>, clock: Clock) =
    "/{bucketName}/{bucketKey:.+}" bind DELETE to routes(
        queryPresent("tagging") bind {
            deleteObjectTagging(it.path("bucketName")!!, buckets, bucketContent, keyFor(it), it.headers, clock)
        },
        otherwise bind { req ->
            deleteObject(req.path("bucketName")!!, buckets, bucketContent, keyFor(req))
        }
    )

private fun deleteObject(
    bucket: String,
    buckets: Storage<Unit>,
    bucketContent: Storage<BucketKeyContent>,
    bucketKey: String
) = (buckets[bucket]
    ?.let {
        if (bucketContent.remove("${bucket}-$bucketKey")) Response(OK)
        else invalidBucketKeyResponse()
    }
    ?: invalidBucketNameResponse())

private fun deleteObjectTagging(
    bucket: String,
    buckets: Storage<Unit>,
    bucketContent: Storage<BucketKeyContent>,
    bucketKey: String,
    headers: Headers,
    clock: Clock,
): Response {
    if (buckets[bucket] == null) return invalidBucketNameResponse()
    val obj = bucketContent["$bucket-$bucketKey"] ?: return invalidBucketKeyResponse()

    bucketContent["$bucket-$bucketKey"] = obj.copy(
        modified = lastModified(headers, clock),
        tags = emptyList()
    )

    return Response(OK)
}

private fun keyFor(req: Request) = req.path("bucketKey")!!
