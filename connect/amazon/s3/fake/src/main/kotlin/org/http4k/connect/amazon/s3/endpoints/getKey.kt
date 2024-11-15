package org.http4k.connect.amazon.s3.endpoints

import org.http4k.connect.amazon.s3.BucketKeyContent
import org.http4k.connect.amazon.s3.action.bodyFor
import org.http4k.connect.amazon.s3.requiresRestore
import org.http4k.connect.amazon.s3.restoreReady
import org.http4k.connect.amazon.s3.storageClass
import org.http4k.connect.storage.Storage
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.util.Base64

internal fun pathBasedBucketGetKey(buckets: Storage<Unit>, bucketContent: Storage<BucketKeyContent>) =
    "/{bucketName}/{bucketKey:.+}" bind Method.GET to routes(
        queryPresent("tagging") bind {
            bucketGetTagging(buckets, it.path("bucketName")!!, bucketContent, keyFor(it))
        },
        otherwise bind { req ->
            bucketGetObject(buckets, req.path("bucketName")!!, bucketContent, keyFor(req))
        }
    )

internal fun bucketGetKey(buckets: Storage<Unit>, bucketContent: Storage<BucketKeyContent>) =
    "/{bucketKey:.+}" bind Method.GET to routes(
        queryPresent("tagging") bind {
            bucketGetTagging(buckets, it.subdomain(buckets), bucketContent, keyFor(it))
        },
        otherwise bind { req ->
            bucketGetObject(buckets, req.subdomain(buckets), bucketContent, keyFor(req))
        }
    )

private fun bucketGetObject(
    buckets: Storage<Unit>,
    bucket: String,
    bucketContent: Storage<BucketKeyContent>,
    bucketKey: String,
): Response {
    if (buckets[bucket] == null) return invalidBucketNameResponse()
    val obj = bucketContent["${bucket}-$bucketKey"] ?: return invalidBucketKeyResponse()
    if (obj.storageClass().requiresRestore() && !obj.restoreReady()) return invalidObjectStateResponse()

    return Base64.getDecoder().decode(obj.content).let { bytes ->
        Response(OK)
            .headers(getHeadersWithoutXHttp4kPrefix(obj))
            .body(bytes.inputStream(), bytes.size.toLong())
    }
}

private fun bucketGetTagging(
    buckets: Storage<Unit>,
    bucket: String,
    bucketContent: Storage<BucketKeyContent>,
    bucketKey: String,
): Response {
    if (buckets[bucket] == null) return invalidBucketNameResponse()
    val obj = bucketContent["${bucket}-$bucketKey"] ?: return invalidBucketKeyResponse()

    return Response(OK).body(bodyFor(obj.tags))
}

private fun keyFor(request: Request) = request.path("bucketKey")!!
