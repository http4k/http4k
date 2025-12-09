import org.http4k.aws.AwsCredentials
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.amazon.s3.model.S3BucketPreSigner
import org.http4k.core.Uri
import java.time.Clock
import java.time.Duration

fun main() {
    // create pre-signer
    val preSigner = S3BucketPreSigner(
        bucketName = BucketName.of("foobar"),
        region = Region.of("us-east-1"),
        credentials = AwsCredentials("accessKeyId", "secretKey")
    )

    val alternateProviderPresigner = S3BucketPreSigner(
        bucketName = BucketName.of("lovely-bucket"),
        region = Region.of("us-west-000"),
        credentials = AwsCredentials("access-key", "secret-key"),
        clock = Clock.systemUTC(),
        overrideEndpoint = Uri.of("https://s3.us-west-000.backblazeb2.com") // region is not interpolated atm.
    )

    val key = BucketKey.of("keyName")

    // create a pre-signed PUT
    val put = preSigner.put(
        key = key,
        duration = Duration.ofMinutes(5), // how long the URL is valid for
        headers = listOf("content-type" to "application.json")  // add optional signed headers
    )
    println(put.uri)

    // create a pre-signed GET
    val get = preSigner.get(
        key = key,
        duration = Duration.ofMinutes(5)
    )
    println(get)

    // share these URIs to your clients so they can perform the operations without credentials
}
