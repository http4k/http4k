package org.http4k.connect.kafka.rest.v3.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.action.NullableKafkaRestAction
import org.http4k.connect.kafka.rest.v3.KafkaRestV3Action
import org.http4k.connect.kafka.rest.v3.model.ClusterId
import org.http4k.connect.kafka.rest.v3.model.Metadata
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class GetTopics(val id: ClusterId) : NullableKafkaRestAction<KafkaTopicList>(kClass()),
    KafkaRestV3Action<KafkaTopicList?> {
    override fun toRequest() = Request(GET, "/kafka/v3/clusters/$id/topics")
}

@JsonSerializable
data class KafkaTopicList(
    val `data`: List<KafkaTopic>,
    val metadata: Metadata
) {
    val kind = "KafkaTopicList"
}
