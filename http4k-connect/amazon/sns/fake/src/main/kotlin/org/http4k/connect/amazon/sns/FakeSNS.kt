package org.http4k.connect.amazon.sns

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.sns.model.MessageAttribute
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeSNS(
    topics: Storage<List<SNSMessage>> = Storage.InMemory(),
    awsAccount: AwsAccount = AwsAccount.of("1234567890"),
    private val region: Region = Region.of("ldn-north-1"),
) : ChaoticHttpHandler() {

    override val app = routes(
        "/" bind POST to routes(
            createTopic(topics, awsAccount, region),
            deleteTopic(topics, awsAccount, region),
            listTopics(topics, awsAccount, region),
            publish(topics, awsAccount, region),
            publishBatch(topics, awsAccount, region)
        )
    )

    /**
     * Convenience function to get a SNS client
     */
    fun client() = SNS.Http(region, { AwsCredentials("accessKey", "secret") }, this)
}

data class SNSMessage(val message: String, val subject: String?, val attributes: List<MessageAttribute>)

fun main() {
    FakeSNS().start()
}
