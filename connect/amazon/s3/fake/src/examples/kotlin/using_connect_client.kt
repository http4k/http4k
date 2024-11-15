package org.http4k.connect.amazon.s3

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.valueOrNull
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.core.HttpHandler
import org.http4k.filter.debug
import java.io.InputStream

const val USE_REAL_CLIENT = false

fun main() {
    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeS3()

    val bucketName = BucketName.of("foobar")
    val bucketKey = BucketKey.of("keyName")
    val region = Region.of("us-east-1")

    // create global and bucket level clients
    val s3 = S3.Http({ AwsCredentials("accessKeyId", "secretKey") }, http.debug())
    val s3Bucket = S3Bucket.Http(bucketName, region, { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // all operations return a Result monad of the API type
    val createResult: Result<Unit, RemoteFailure> = s3.createBucket(bucketName, region)
    createResult.valueOrNull()!!

    // we can store some content in the bucket...
    val putResult: Result<Unit, RemoteFailure> =
        s3Bucket.putObject(bucketKey, "hellothere".byteInputStream(), emptyList())
    putResult.valueOrNull()!!

    // and get back the content which we stored
    val getResult: Result<InputStream?, RemoteFailure> = s3Bucket.get(bucketKey)
    val content: InputStream = getResult.valueOrNull()!!
    println(content.reader().readText())
}
