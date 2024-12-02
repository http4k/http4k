package org.http4k.connect.amazon.s3

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.s3.endpoints.bucketDeleteBucket
import org.http4k.connect.amazon.s3.endpoints.bucketDeleteKey
import org.http4k.connect.amazon.s3.endpoints.bucketGetKey
import org.http4k.connect.amazon.s3.endpoints.bucketHeadBucket
import org.http4k.connect.amazon.s3.endpoints.bucketHeadKey
import org.http4k.connect.amazon.s3.endpoints.bucketListObjectsV2
import org.http4k.connect.amazon.s3.endpoints.bucketPostKey
import org.http4k.connect.amazon.s3.endpoints.bucketPutBucket
import org.http4k.connect.amazon.s3.endpoints.bucketPutKey
import org.http4k.connect.amazon.s3.endpoints.copyKey
import org.http4k.connect.amazon.s3.endpoints.globalHeadBucket
import org.http4k.connect.amazon.s3.endpoints.globalListBuckets
import org.http4k.connect.amazon.s3.endpoints.globalListObjectsV2
import org.http4k.connect.amazon.s3.endpoints.globalPutBucket
import org.http4k.connect.amazon.s3.endpoints.pathBasedBucketDeleteKey
import org.http4k.connect.amazon.s3.endpoints.pathBasedBucketGetKey
import org.http4k.connect.amazon.s3.endpoints.pathBasedBucketHeadKey
import org.http4k.connect.amazon.s3.endpoints.pathBasedBucketPostKey
import org.http4k.connect.amazon.s3.endpoints.pathBasedBucketPutKey
import org.http4k.connect.amazon.s3.endpoints.pathBasedCopyKey
import org.http4k.connect.amazon.s3.endpoints.subdomain
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.routing.Predicate
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock

/**
 * Global S3 operations (manage buckets)
 */
class FakeS3(
    private val buckets: Storage<Unit> = Storage.InMemory(),
    private val bucketContent: Storage<BucketKeyContent> = Storage.InMemory(),
    private val clock: Clock = Clock.systemUTC()
) : ChaoticHttpHandler() {

    private val isS3 = Predicate("", Status.NOT_FOUND, { it: Request -> it.subdomain(buckets) == "s3" })
    private val isBucket = Predicate("", Status.NOT_FOUND, { it: Request -> it.subdomain(buckets) != "s3" })

    override val app = routes(
        isS3 bind routes(
            pathBasedCopyKey(buckets, bucketContent, clock),
            pathBasedBucketGetKey(buckets, bucketContent),
            pathBasedBucketPostKey(buckets, bucketContent, clock),
            pathBasedBucketPutKey(buckets, bucketContent, clock),
            pathBasedBucketDeleteKey(buckets, bucketContent, clock),
            pathBasedBucketHeadKey(buckets, bucketContent),
            globalListObjectsV2(buckets, bucketContent),
            globalHeadBucket(buckets),
            globalPutBucket(buckets),
            globalListBuckets(buckets)
        ),
        isBucket bind routes(
            copyKey(buckets, bucketContent, clock),
            bucketHeadBucket(buckets),
            bucketHeadKey(buckets, bucketContent),
            bucketGetKey(buckets, bucketContent),
            bucketPostKey(buckets, bucketContent, clock),
            bucketPutKey(buckets, bucketContent, clock),
            bucketDeleteKey(buckets, bucketContent, clock),
            bucketPutBucket(buckets),
            bucketDeleteBucket(buckets),
            bucketListObjectsV2(buckets, bucketContent)
        )
    )

    /**
     * Convenience function to get an S3 client for global operations
     */
    fun s3Client() = S3.Http({ AwsCredentials("accessKey", "secret") }, this, clock)

    /**
     * Convenience function to get an S3 client for bucket operations
     */
    fun s3BucketClient(name: BucketName, region: Region) = S3Bucket.Http(
        name,
        region,
        { AwsCredentials("accessKey", "secret") }, this, clock
    )
}

fun main() {
    FakeS3().start()
}
