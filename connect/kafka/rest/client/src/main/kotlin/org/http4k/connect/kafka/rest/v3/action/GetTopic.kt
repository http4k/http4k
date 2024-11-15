package org.http4k.connect.kafka.rest.v3.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.action.NullableKafkaRestAction
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v3.KafkaRestV3Action
import org.http4k.connect.kafka.rest.v3.model.ClusterId
import org.http4k.connect.kafka.rest.v3.model.Metadata
import org.http4k.connect.kafka.rest.v3.model.Relation
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class GetTopic(val id: ClusterId, val topic: Topic) : NullableKafkaRestAction<KafkaTopic>(kClass()),
    KafkaRestV3Action<KafkaTopic?> {
    override fun toRequest() = Request(GET, "/kafka/v3/clusters/$id/topics/$topic")
}

@JsonSerializable
data class KafkaTopic(
    val cluster_id: ClusterId,
    val topic_name: Topic,
    val metadata: Metadata,
    val is_internal: Boolean,
    val replication_factor: Int,
    val partitions_count: Int,
    val partitions: Relation,
    val configs: Relation,
    val partition_reassignments: Relation,
    val authorized_operations: List<Any>
) {
    val kind = "KafkaTopic"
}
