import dev.forkhandles.result4k.Result
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.sns.FakeSNS
import org.http4k.connect.amazon.sns.Http
import org.http4k.connect.amazon.sns.SNS
import org.http4k.connect.amazon.sns.action.PublishedMessage
import org.http4k.connect.amazon.sns.model.TopicName
import org.http4k.connect.amazon.sns.publishMessage
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    val region = Region.of("us-east-1")
    val topic = TopicName.of("myTopic")
    val topicArn = ARN.of(SNS.awsService, region, AwsAccount.of("000000001"), topic)

    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeSNS()

    // create a client
    val client = SNS.Http(region, { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // all operations return a Result monad of the API type
    val publishedMessage: Result<PublishedMessage, RemoteFailure> = client.publishMessage(
        "hello!", topicArn = topicArn
    )
    println(publishedMessage)

}
