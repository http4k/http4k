import dev.forkhandles.result4k.Result
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.RoleSessionName
import org.http4k.connect.amazon.sts.FakeSTS
import org.http4k.connect.amazon.sts.Http
import org.http4k.connect.amazon.sts.STS
import org.http4k.connect.amazon.sts.action.AssumedRole
import org.http4k.connect.amazon.sts.assumeRole
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

private const val USE_REAL_CLIENT = false

fun main() {

    val region = Region.of("us-east-1")
    val roleArn = ARN.of("arn:aws:sts:us-east-1:000000000001:role:myrole")

    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeSTS()

    // create a client
    val client = STS.Http(region, { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // all operations return a Result monad of the API type
    val assumeRoleResult: Result<AssumedRole, RemoteFailure> =
        client.assumeRole(roleArn, RoleSessionName.of("sessionId"))
    println(assumeRoleResult)
}
