package org.http4k.connect.kafka.rest.v2.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.action.NullableKafkaRestAction
import org.http4k.connect.kafka.rest.model.BrokerId
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v2.KafkaRestV2Action
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class GetPartitions(
    val topic: Topic
) : NullableKafkaRestAction<Array<Partition>>(kClass()), KafkaRestV2Action<Array<Partition>?> {
    override fun toRequest() = Request(GET, "/topics/$topic/partitions")
}

@JsonSerializable
data class Replica(
    val broker: BrokerId,
    val leader: Boolean,
    val in_sync: Boolean
)

@JsonSerializable
data class Partition(val partition: PartitionId, val leader: BrokerId, val replicas: List<Replica>)
