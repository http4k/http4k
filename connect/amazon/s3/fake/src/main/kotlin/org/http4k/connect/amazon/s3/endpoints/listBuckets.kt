package org.http4k.connect.amazon.s3.endpoints

import org.http4k.connect.amazon.s3.ListAllMyBuckets
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.storage.Storage
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.routing.bind

fun globalListBuckets(buckets: Storage<Unit>) = "/" bind Method.GET to {
    Response(Status.OK)
        .with(s3ErrorLens of ListAllMyBuckets(buckets.keySet("").map { BucketName.of(it) }.toList().sortedBy { it.value }))
}
