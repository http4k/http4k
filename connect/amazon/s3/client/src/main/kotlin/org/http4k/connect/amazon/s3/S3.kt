package org.http4k.connect.amazon.s3

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion
import org.http4k.connect.amazon.s3.action.GetObject
import org.http4k.connect.amazon.s3.action.PutObject
import org.http4k.connect.amazon.s3.model.BucketKey
import java.io.InputStream

/**
 * Docs: https://docs.aws.amazon.com/AmazonS3/latest/API/Welcome.html
 */
@Http4kConnectApiClient
interface S3 {
    operator fun <R> invoke(action: S3Action<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("s3")
}

/**
 * Interface for bucket-specific S3 operations
 */
@Http4kConnectApiClient
interface S3Bucket {
    operator fun <R> invoke(action: S3BucketAction<R>): Result<R, RemoteFailure>

    operator fun get(key: BucketKey): Result<InputStream?, RemoteFailure> = this(GetObject(key))
    operator fun set(key: BucketKey, content: InputStream): Result<Unit, RemoteFailure> = this(PutObject(key, content))

    companion object : AwsServiceCompanion("s3")
}

