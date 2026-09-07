import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.cognitoidentity.CognitoIdentity
import org.http4k.connect.amazon.cognitoidentity.FakeCognitoIdentity
import org.http4k.connect.amazon.cognitoidentity.Http
import org.http4k.connect.amazon.cognitoidentity.action.Identity
import org.http4k.connect.amazon.cognitoidentity.getCredentialsForIdentity
import org.http4k.connect.amazon.cognitoidentity.getId
import org.http4k.connect.amazon.cognitoidentity.model.IdentityPoolId
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    val region = Region.of("us-east-1")

    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeCognitoIdentity()

    // create a client
    val client = CognitoIdentity.Http(region, { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // all operations return a Result monad of the API type
    val identity: Result<Identity, RemoteFailure> = client.getId(
        IdentityPoolId.of("us-east-1:12345678-1234-1234-1234-123456789012")
    )
    println(identity)

    identity.map { println(client.getCredentialsForIdentity(it.IdentityId)) }
}
