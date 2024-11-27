package org.http4k.connect.amazon.s3.endpoints

import org.http4k.connect.amazon.s3.BucketKeyContent
import org.http4k.connect.amazon.s3.ListBucketResult
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.routing.bind
import org.http4k.routing.path

fun bucketListObjectsV2(buckets: Storage<Unit>, bucketContent: Storage<BucketKeyContent>) =
    "/" bind GET to { listObjectsV2(it.subdomain(buckets), buckets, bucketContent) }

fun globalListObjectsV2(buckets: Storage<Unit>, bucketContent: Storage<BucketKeyContent>) =
    "/{bucketName}" bind GET to {
        listObjectsV2(
            it.path("bucketName")!!,
            buckets, bucketContent
        )
    }

fun listObjectsV2(bucket: String, buckets: Storage<Unit>, bucketContent: Storage<BucketKeyContent>) =
    buckets[bucket]
        ?.let {
            Response(Status.OK)
                .with(
                    s3ErrorLens of ListBucketResult(
                        bucket,
                        bucketContent.keySet(bucket)
                            .map { it.removePrefix("$bucket-") }
                            .map { bucketContent["$bucket-$it"]!! }
                            .sortedBy { it.key.value }
                    ))
        }
        ?: Response(Status.NOT_FOUND)
