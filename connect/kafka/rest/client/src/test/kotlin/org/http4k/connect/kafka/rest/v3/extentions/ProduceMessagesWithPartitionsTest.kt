package org.http4k.connect.kafka.rest.v3.extentions

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import dev.forkhandles.values.ZERO
import org.http4k.connect.kafka.rest.FakeKafkaRest
import org.http4k.connect.kafka.rest.Http
import org.http4k.connect.kafka.rest.KafkaRest
import org.http4k.connect.kafka.rest.extensions.RoundRobinRecordPartitioner
import org.http4k.connect.kafka.rest.model.Offset
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v3.action.ProducedRecord
import org.http4k.connect.kafka.rest.v3.action.ProducedRecordData
import org.http4k.connect.kafka.rest.v3.extensions.produceRecordsWithPartitions
import org.http4k.connect.kafka.rest.v3.model.ClusterId
import org.http4k.connect.kafka.rest.v3.model.Record
import org.http4k.connect.kafka.rest.v3.model.RecordData.Binary
import org.http4k.connect.kafka.rest.v3.model.RecordFormat.BINARY
import org.http4k.connect.model.Base64Blob
import org.http4k.core.Credentials
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.time.Instant.EPOCH

class ProduceRecordeWithPartitionsTest {

    @Test
    fun `writing to a list of partitions using a partitioner`() {
        val kafkaRest = KafkaRest.Http(Credentials("", ""), Uri.of(""), FakeKafkaRest())

        val clusterId = ClusterId.of("foobar")
        val topic = Topic.of("topic")

        assertThat(
            kafkaRest.produceRecordsWithPartitions(
                topic,
                clusterId,
                listOf(
                    Record(Binary(Base64Blob.encode("foo1"))),
                    Record(Binary(Base64Blob.encode("foo2"))),
                    Record(Binary(Base64Blob.encode("foo3"))),
                ),
                ::RoundRobinRecordPartitioner
            )
                .valueOrNull()!!.toList(),
            equalTo(
                listOf(
                    ProducedRecord(
                        OK, clusterId, topic, PartitionId.ZERO, Offset.of(1), EPOCH,
                        ProducedRecordData(BINARY, 8), null
                    ),
                    ProducedRecord(
                        OK, clusterId, topic, PartitionId.ZERO, Offset.of(2), EPOCH,
                        ProducedRecordData(BINARY, 8), null
                    ),
                    ProducedRecord(
                        OK, clusterId, topic, PartitionId.ZERO, Offset.of(3), EPOCH,
                        ProducedRecordData(BINARY, 8), null
                    )
                )
            )
        )
    }
}

