import dev.forkhandles.result4k.Result
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.cloudfront.CloudFront
import org.http4k.connect.amazon.cloudfront.FakeCloudFront
import org.http4k.connect.amazon.cloudfront.Http
import org.http4k.connect.amazon.cloudfront.createInvalidation
import org.http4k.connect.amazon.cloudfront.model.CallerReference.Companion.random
import org.http4k.connect.amazon.cloudfront.model.DistributionId
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeCloudFront()

    // create a client
    val client =
        CloudFront.Http({ AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // all operations return a Result monad of the API type
    val result: Result<Unit, RemoteFailure> = client
        .createInvalidation(DistributionId.of("a-distribution-id"), listOf("/path"), 1, random())
}
