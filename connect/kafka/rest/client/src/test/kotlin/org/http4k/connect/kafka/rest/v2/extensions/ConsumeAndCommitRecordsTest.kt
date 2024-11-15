package org.http4k.connect.kafka.rest.v2.extensions

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.failureOrNull
import dev.forkhandles.values.ZERO
import org.http4k.connect.kafka.rest.FakeKafkaRest
import org.http4k.connect.kafka.rest.Http
import org.http4k.connect.kafka.rest.KafkaRest
import org.http4k.connect.kafka.rest.extensions.RecordConsumer
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerInstance
import org.http4k.connect.kafka.rest.model.Offset
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v2.createConsumer
import org.http4k.connect.kafka.rest.v2.getOffsets
import org.http4k.connect.kafka.rest.v2.model.CommitOffset
import org.http4k.connect.kafka.rest.v2.model.CommitOffsetsSet
import org.http4k.connect.kafka.rest.v2.model.Consumer
import org.http4k.connect.kafka.rest.v2.model.PartitionOffsetRequest
import org.http4k.connect.kafka.rest.v2.model.Record
import org.http4k.connect.kafka.rest.v2.model.RecordFormat.json
import org.http4k.connect.kafka.rest.v2.model.Records
import org.http4k.connect.kafka.rest.v2.produceMessages
import org.http4k.connect.kafka.rest.v2.subscribeToTopics
import org.http4k.connect.successValue
import org.http4k.core.Credentials
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class ConsumeAndCommitRecordsTest {
    private val kafkaRest = KafkaRest.Http(
        Credentials("", ""), Uri.of(""), FakeKafkaRest()
    )

    private val group = ConsumerGroup.of("group")
    private val instance = ConsumerInstance.of("foo")
    private val topic = Topic.of("topic")

    @Test
    fun `consumer completes and commits successful records`() {
        consumeAndCommitUsing { Success(Unit) }.successValue()

        assertThat(
            kafkaRest.getOffsets(group, instance, listOf(PartitionOffsetRequest(topic, PartitionId.ZERO)))
                .successValue(),
            equalTo(CommitOffsetsSet(listOf(CommitOffset(topic, PartitionId.ZERO, Offset.of(3)))))
        )
    }

    @Test
    fun `on failure, consumer seeks failed record`() {
        consumeAndCommitUsing {
            when (it.toInt()) {
                0 -> Success(Unit)
                else -> Failure(Exception())
            }
        }.failureOrNull()!!

        assertThat(
            kafkaRest.getOffsets(group, instance, listOf(PartitionOffsetRequest(topic, PartitionId.ZERO)))
                .successValue(),
            equalTo(CommitOffsetsSet(listOf(CommitOffset(topic, PartitionId.ZERO, Offset.of(1)))))
        )
    }

    private fun consumeAndCommitUsing(recordConsumer: RecordConsumer<String>) = with(kafkaRest) {
        createConsumer(group, Consumer(instance, json)).successValue()
        subscribeToTopics(group, instance, listOf(topic)).successValue()
        produceMessages(
            topic, Records.Json(
                listOf(
                    Record("0", "0"),
                    Record("1", "1"),
                    Record("2", "2")
                )
            )
        ).successValue()
        consumeAndCommitRecords(
            group,
            instance,
            topic, json, recordConsumer
        )
    }
}
