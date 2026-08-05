package org.http4k.connect.amazon.sqs

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.AwsRestJsonFake
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.sqs.model.QueueName
import org.http4k.connect.amazon.sqs.model.SQSMessage
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock
import java.util.concurrent.atomic.AtomicLong

class FakeSQS(
    private val queues: Storage<List<SQSMessage>> = Storage.InMemory(),
    awsAccount: AwsAccount = AwsAccount.of("1234567890"),
    private val region: Region = Region.of("ldn-north-1"),
    private val deduplication: Storage<DeduplicationRecord> = Storage.InMemory(),
    private val queueConfig: Storage<QueueConfig> = Storage.InMemory(),
    private val clock: Clock = Clock.systemUTC()
) : ChaoticHttpHandler() {

    private val api = AwsRestJsonFake(SqsMoshi, AwsService.of("sqs"), region, awsAccount)

    private val sequenceNumbers = AtomicLong()

    override val app = routes(
        "/" bind POST to routes(
            api.deleteMessage(queues),
            api.deleteQueue(queues, deduplication, queueConfig),
            api.deleteMessageBatch(queues),
            api.receiveMessage(queues),
            api.createQueue(queues, queueConfig, awsAccount),
            api.getQueueAttributes(queues),
            api.sendMessage(queues, deduplication, queueConfig, clock, sequenceNumbers),
            api.sendMessageBatch(queues, deduplication, queueConfig, clock, sequenceNumbers),
            api.listQueues(region, awsAccount, queues)
        )
    )

    /**
     * Convenience function to get a SQS client
     */
    fun client() = SQS.Http(region, { AwsCredentials("accessKey", "secret") }, this)

    fun listMessages(queueName: QueueName) = queues[queueName.value].orEmpty()
}

fun main() {
    FakeSQS().start()
}
