import dev.forkhandles.result4k.Result
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.sqs.FakeSQS
import org.http4k.connect.amazon.sqs.Http
import org.http4k.connect.amazon.sqs.SQS
import org.http4k.connect.amazon.sqs.action.CreatedQueue
import org.http4k.connect.amazon.sqs.createQueue
import org.http4k.connect.amazon.sqs.model.QueueName
import org.http4k.connect.amazon.sqs.receiveMessage
import org.http4k.connect.amazon.sqs.sendMessage
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    val region = Region.of("us-east-1")
    val queueName = QueueName.of("myqueue")

    val queueUrl = Uri.of("https://sqs.us-east-1.amazonaws.com/000000001/$queueName")

    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeSQS()

    // create a client
    val client = SQS.Http(region, { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // all operations return a Result monad of the API type
    val createdQueueResult: Result<CreatedQueue, RemoteFailure> = client.createQueue(queueName, emptyList(), emptyMap())
    println(createdQueueResult)

    // send a message

    println(client.sendMessage(queueUrl, "hello"))

    // and receive it..
    println(client.receiveMessage(queueUrl))
}
