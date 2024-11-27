package org.http4k.connect.amazon.sns.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.Paged
import org.http4k.connect.PagedAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.sequenceOfNodes
import org.http4k.connect.amazon.core.text
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.sns.SNSAction
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Response

@Http4kConnectAction
data class ListTopics(val NextToken: String? = null) : SNSAction<TopicList>("ListTopics",
    NextToken?.let { "NextToken" to NextToken }
),
    PagedAction<String, ARN, TopicList, ListTopics> {

    override fun next(token: String) = copy(NextToken = token)

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(
                with(xmlDoc()) {
                    val list = getElementsByTagName("TopicArn")
                        .sequenceOfNodes()
                        .map { ARN.of(it.text()) }
                        .toList()
                    TopicList(list, getElementsByTagName("NextToken").item(0)?.text())
                }
            )

            else -> Failure(asRemoteFailure(this))
        }
    }
}

data class TopicList(
    override val items: List<ARN>,
    val NextToken: String? = null
) : Paged<String, ARN> {
    override fun token() = NextToken
}
