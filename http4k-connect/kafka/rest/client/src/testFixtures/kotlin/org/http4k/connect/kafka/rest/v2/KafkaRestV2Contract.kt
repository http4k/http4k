package org.http4k.connect.kafka.rest.v2

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import dev.forkhandles.values.ZERO
import http4k.RandomEvent
import http4k.RandomKey
import org.http4k.connect.kafka.rest.Http
import org.http4k.connect.kafka.rest.KafkaRest
import org.http4k.connect.kafka.rest.KafkaRestConsumer
import org.http4k.connect.kafka.rest.KafkaRestMoshi
import org.http4k.connect.kafka.rest.KafkaRestMoshi.asFormatString
import org.http4k.connect.kafka.rest.consumeRecordsTwiceBecauseOfProxy
import org.http4k.connect.kafka.rest.consumerRecordsTwiceBecauseOfProxy
import org.http4k.connect.kafka.rest.model.AutoCommitEnable.`false`
import org.http4k.connect.kafka.rest.model.AutoOffsetReset.earliest
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerInstance
import org.http4k.connect.kafka.rest.model.Offset
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.randomString
import org.http4k.connect.kafka.rest.v2.model.CommitOffset
import org.http4k.connect.kafka.rest.v2.model.CommitOffsetsSet
import org.http4k.connect.kafka.rest.v2.model.Consumer
import org.http4k.connect.kafka.rest.v2.model.PartitionOffsetRequest
import org.http4k.connect.kafka.rest.v2.model.Record
import org.http4k.connect.kafka.rest.v2.model.RecordFormat
import org.http4k.connect.kafka.rest.v2.model.RecordFormat.avro
import org.http4k.connect.kafka.rest.v2.model.RecordFormat.binary
import org.http4k.connect.kafka.rest.v2.model.RecordFormat.json
import org.http4k.connect.kafka.rest.v2.model.Records
import org.http4k.connect.kafka.rest.v2.model.SeekOffset
import org.http4k.connect.kafka.rest.v2.model.TopicRecord
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.successValue
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

interface KafkaRestV2Contract {

    val http: HttpHandler
    val uri: Uri

    private val kafkaRest get() = KafkaRest.Http(Credentials("", ""), uri, http)

    @BeforeEach
    fun `can get to proxy v2`() {
        assumeTrue(http(Request(GET, uri)).status == OK)
    }

    @Test
    fun `can send JSON messages and get them back`() {
        kafkaRest.testSending(json, { it.records.first() }) {
            Records.Json(listOf(Record(it, Message(randomString()))))
        }
    }

    @Test
    open fun `can send AVRO messages and get them back`() {
        kafkaRest.testSending(
            avro,
            { it.records.first() },
            { it?.toMapOrString() },
        ) {
            Records.Avro(
                listOf(
                    Record(
                        RandomKey(UUID.nameUUIDFromBytes(it.toByteArray())),
                        RandomEvent(
                            LocalDate.EPOCH,
                            LocalTime.MIDNIGHT,
                            Instant.EPOCH,
                            LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `can send BINARY messages and get them back`() {
        kafkaRest.testSending(binary, { it.records.first() }) {
            Records.Binary(listOf(Record(Base64Blob.encode(it), Base64Blob.encode(randomString()))))
        }
    }

    @Test
    fun `can create consumer client`() {
        val topic1 = Topic.of("t1_${randomString()}")

        val group = ConsumerGroup.of(randomString())
        val name = ConsumerInstance.of(randomString())
        val consumer = KafkaRestConsumer.Http(
            Credentials("", ""), group,
            Consumer(name, json, earliest), uri, http
        ).successValue()

        try {
            consumer.subscribeToTopics(listOf(topic1)).successValue()

            val record1 = Records.Json(listOf(Record("m1", Message(randomString()))))
            kafkaRest.produceMessages(topic1, record1).successValue()

            assertThat(
                consumer.consumeRecordsTwiceBecauseOfProxy(json).toList(),
                equalTo(
                    listOf(
                        TopicRecord(
                            topic1, record1.records.first().key,
                            record1.records.first().value.toMapOrString(), PartitionId.of(0), Offset.of(0)
                        )
                    )
                )
            )
        } finally {
            consumer.delete().successValue()
        }
    }

    @Test
    fun `can manually commit consumer offsets for group`() {
        val topic1 = Topic.of("t1_${randomString()}")

        val group = ConsumerGroup.of(randomString())

        val credentials = Credentials("", "")

        fun consumer1Consumes2RecordsAndDoesNotCommit() {
            val consumer1 = KafkaRestConsumer.Http(
                credentials, group,
                Consumer(ConsumerInstance.of("--1"), json, earliest, enableAutocommit = `false`), uri, http
            ).successValue()

            consumer1.subscribeToTopics(listOf(topic1)).successValue()

            assertThat(
                consumer1.getOffsets(listOf(PartitionOffsetRequest(topic1, PartitionId.ZERO))).successValue(),
                equalTo(CommitOffsetsSet(listOf()))
            )

            val records = consumer1.consumeRecordsTwiceBecauseOfProxy(json)
            assertThat(records.map { it.key }, equalTo(listOf("m1", "m2")))

            assertThat(
                consumer1.getOffsets(listOf(PartitionOffsetRequest(topic1, PartitionId.ZERO))).successValue(),
                equalTo(CommitOffsetsSet(listOf()))
            )

            consumer1.delete().successValue()
        }

        fun consumer2GetsTheSame2RecordsAndCommitsAt2() {
            val consumer2 = KafkaRestConsumer.Http(
                credentials, group,
                Consumer(ConsumerInstance.of("--2"), json, earliest, enableAutocommit = `false`), uri, http
            ).successValue()

            consumer2.subscribeToTopics(listOf(topic1)).successValue()

            val records = consumer2.consumeRecordsTwiceBecauseOfProxy(json)
            assertThat(records.map { it.key }, equalTo(listOf("m1", "m2")))

            assertThat(
                consumer2.getOffsets(listOf(PartitionOffsetRequest(topic1, PartitionId.ZERO))).successValue(),
                equalTo(CommitOffsetsSet(listOf()))
            )

            consumer2.commitOffsets(
                listOf(CommitOffset(topic1, PartitionId.of(0), records.last().offset))
            ).successValue()

            assertThat(
                consumer2.getOffsets(listOf(PartitionOffsetRequest(topic1, PartitionId.ZERO))).successValue(),
                equalTo(
                    CommitOffsetsSet(
                        listOf(
                            CommitOffset(topic1, PartitionId.ZERO, Offset.of(2), "")
                        )
                    )
                )
            )

            consumer2.delete().successValue()
        }

        fun consumer3GetsOnly2NewRecords() {
            val consumer3 = KafkaRestConsumer.Http(
                credentials, group,
                Consumer(ConsumerInstance.of("--3"), json, earliest, enableAutocommit = `false`), uri, http
            ).successValue()

            consumer3.subscribeToTopics(listOf(topic1)).successValue()

            assertThat(consumer3.consumeRecordsTwiceBecauseOfProxy(json).map { it.key }, equalTo(listOf("m3", "m4")))

            assertThat(
                consumer3.getOffsets(listOf(PartitionOffsetRequest(topic1, PartitionId.ZERO))).successValue(),
                equalTo(
                    CommitOffsetsSet(listOf(CommitOffset(topic1, PartitionId.ZERO, Offset.of(2), "")))
                )
            )

            consumer3.delete().successValue()
        }

        kafkaRest.produceMessages(
            topic1, Records.Json(
                listOf(
                    Record("m1", Message(randomString())),
                    Record("m2", Message(randomString()))
                )
            )
        ).successValue()

        consumer1Consumes2RecordsAndDoesNotCommit()

        consumer2GetsTheSame2RecordsAndCommitsAt2()

        kafkaRest.produceMessages(
            topic1, Records.Json(
                listOf(
                    Record("m3", Message(randomString())),
                    Record("m4", Message(randomString()))
                )
            )
        ).successValue()

        consumer3GetsOnly2NewRecords()
    }

    @Test
    fun `can seek back to earlier next point`() {
        val topic1 = Topic.of("t1_${randomString()}")

        val group = ConsumerGroup.of(randomString())

        val credentials = Credentials("", "")

        kafkaRest.produceMessages(
            topic1, Records.Json(
                listOf(
                    Record("m1", Message(randomString())),
                    Record("m2", Message(randomString()))
                )
            )
        ).successValue()

        val consumer = KafkaRestConsumer.Http(
            credentials, group,
            Consumer(ConsumerInstance.of("--1"), json, earliest, enableAutocommit = `false`), uri, http
        ).successValue()

        consumer.subscribeToTopics(listOf(topic1)).successValue()

        val records = consumer.consumeRecordsTwiceBecauseOfProxy(json)

        consumer.seekOffsets(records.map { SeekOffset(topic1, it.partition, Offset.ZERO) }).successValue()

        val records2 = consumer.consumeRecordsTwiceBecauseOfProxy(json)

        assertThat(records2.map { it.key }, equalTo(listOf("m1", "m2")))

        consumer.delete().successValue()
    }

    private fun <K : Any, V : Any, T : Record<K, V>> KafkaRest.testSending(
        format: RecordFormat,
        recordFrom: (Records) -> T,
        convert: (Any?) -> Any? = { it },
        buildRecords: (String) -> Records
    ) {
        val topic1 = Topic.of("t1_${randomString()}")
        val topic2 = Topic.of("t2_${randomString()}")
        val topic3 = Topic.of("t3_${randomString()}")

        val group = ConsumerGroup.of(randomString())
        val name = ConsumerInstance.of(randomString())

        val instance = createConsumer(group, Consumer(name, format, earliest)).successValue().instance_id

        try {
            subscribeToTopics(group, instance, listOf(topic1, topic2)).successValue()

            val record1 = buildRecords("m1")
            val record2 = buildRecords("m2")
            val record3 = buildRecords("m3")
            val record4 = buildRecords("m4")
            val record5 = buildRecords("m5")

            produceMessages(topic1, record1).successValue()
            produceMessages(topic2, record2).successValue()
            produceMessages(topic3, record3).successValue()
            produceMessages(topic1, record4).successValue()

            assertThat(
                consumerRecordsTwiceBecauseOfProxy(group, instance, format).toString(),
                equalTo(
                    listOf(
                        TopicRecord(
                            topic1, convert(recordFrom(record1).key),
                            recordFrom(record1).value.toMapOrString(), PartitionId.of(0), Offset.of(0)
                        ),
                        TopicRecord(
                            topic2, convert(recordFrom(record2).key),
                            recordFrom(record2).value.toMapOrString(), PartitionId.of(0), Offset.of(0)
                        ),
                        TopicRecord(
                            topic1, convert(recordFrom(record4).key),
                            recordFrom(record4).value.toMapOrString(), PartitionId.of(0), Offset.of(1)
                        )
                    )
                        .sortedBy { it.key.toString() }
                        .toString()
                )
            )

            assertThat(
                consumerRecordsTwiceBecauseOfProxy(group, instance, format),
                equalTo(emptyList())
            )

            produceMessages(topic1, record5).successValue()

            assertThat(
                consumerRecordsTwiceBecauseOfProxy(group, instance, format).toString(),
                equalTo(
                    listOf(
                        TopicRecord(
                            topic1, convert(recordFrom(record5).key),
                            recordFrom(record5).value.toMapOrString(), PartitionId.of(0), Offset.of(2)
                        ),
                    ).toString()
                )
            )

            assertThat(
                getPartitions(topic1).successValue()!!.toList().size,
                greaterThan(0)
            )

        } finally {
            deleteConsumer(group, instance)
        }
    }

}

private fun <V : Any> V.toMapOrString(): Any {
    return when (this) {
        is Base64Blob -> value
        is String -> this
        else -> KafkaRestMoshi.asA<Map<String, Any>>(asFormatString(this))
    }
}

data class Message(val field: String)
