package org.http4k.connect.amazon.sns.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.core.text
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.sns.SNSAction
import org.http4k.connect.amazon.sns.model.TopicName
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Response

@Http4kConnectAction
data class CreateTopic(
    val topicName: TopicName,
    val tags: List<Tag> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
) : SNSAction<CreatedTopic>(
    "CreateTopic",
    *(tags
        .flatMapIndexed { i, tag ->
            listOf(
                "Tag.member.${i + 1}.Key" to tag.Key,
                "Tag.member.${i + 1}.Value" to tag.Value
            )
        } +
        attributes.entries
            .flatMapIndexed { i, it ->
                listOf(
                    "Attribute.entry.${i + 1}.key" to it.key,
                    "Attribute.entry.${i + 1}.value" to it.value
                )
            } +
        listOf("Name" to topicName.value)
        ).toTypedArray()
) {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(CreatedTopic.from(response))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

data class CreatedTopic(val topicArn: ARN) {
    companion object {
        fun from(response: Response) = with(response) {
            CreatedTopic(ARN.of(xmlDoc().text("TopicArn")))
        }
    }
}
