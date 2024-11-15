package org.http4k.connect.amazon.sns.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.sns.SNSAction
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Response

@Http4kConnectAction
data class DeleteTopic(val topicArn: ARN) : SNSAction<Unit>(
    "DeleteTopic",
    "TopicArn" to topicArn.value
) {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }
}
