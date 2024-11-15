package org.http4k.connect.kafka.rest.v3.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.action.NullableKafkaRestAction
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v3.KafkaRestV3Action
import org.http4k.connect.kafka.rest.v3.model.ClusterId
import org.http4k.connect.kafka.rest.v3.model.Metadata
import org.http4k.connect.kafka.rest.v3.model.Relation
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class GetPartitions(val id: ClusterId, val topic: Topic) : NullableKafkaRestAction<KafkaPartitionList>(kClass()),
    KafkaRestV3Action<KafkaPartitionList?> {
    override fun toRequest() = Request(GET, "/kafka/v3/clusters/$id/topics/$topic/partitions")
}

@JsonSerializable
data class KafkaPartition(
    val cluster_id: ClusterId,
    val metadata: Metadata,
    val topic_name: Topic,
    val partition_id: PartitionId,
    val leader: Relation,
    val replicas: Relation,
    val reassignment: Relation
) {
    val kind = "KafkaPartition"
}

@JsonSerializable
data class KafkaPartitionList(
    val `data`: List<KafkaPartition>,
    val metadata: Metadata
) {
    val kind = "KafkaPartitionList"
}
