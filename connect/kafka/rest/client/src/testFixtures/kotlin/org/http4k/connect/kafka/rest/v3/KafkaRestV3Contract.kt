package org.http4k.connect.kafka.rest.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.kafka.rest.Http
import org.http4k.connect.kafka.rest.KafkaRest
import org.http4k.connect.kafka.rest.KafkaRestConsumer
import org.http4k.connect.kafka.rest.consumeRecordsTwiceBecauseOfProxy
import org.http4k.connect.kafka.rest.model.AutoCommitEnable
import org.http4k.connect.kafka.rest.model.AutoOffsetReset
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerInstance
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v2.model.Consumer
import org.http4k.connect.kafka.rest.v2.model.RecordFormat
import org.http4k.connect.kafka.rest.v2.subscribeToTopics
import org.http4k.connect.kafka.rest.v3.model.ClusterId
import org.http4k.connect.kafka.rest.v3.model.Record
import org.http4k.connect.kafka.rest.v3.model.RecordData
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.successValue
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

interface KafkaRestV3Contract {

    val http: HttpHandler
    val uri: Uri

    private val kafkaRest get() = KafkaRest.Http(Credentials("", ""), uri, http)

    @BeforeEach
    fun `can get to proxy v3`() {
        Assumptions.assumeTrue(http(Request(Method.GET, uri)).status == Status.OK)
    }

    @Test
    fun `send and receive BINARY records`() {
        val clusterId = ClusterId.of("foo")
        val topic = Topic.of("bar")
        val kafkaTopic = kafkaRest.getTopic(clusterId, topic).successValue()
        assertThat(kafkaTopic?.cluster_id, equalTo(clusterId))
        assertThat(kafkaTopic?.topic_name, equalTo(topic))
        assertThat(kafkaTopic?.metadata?.self?.path, equalTo("/kafka/v3/clusters/foo/topics/bar"))

        val partitionList = kafkaRest.getPartitions(clusterId, topic).successValue()
        assertThat(partitionList?.`data`?.first()?.cluster_id, equalTo(clusterId))
        assertThat(partitionList?.`data`?.first()?.topic_name, equalTo(topic))
        assertThat(
            partitionList?.`data`?.first()?.metadata?.self?.path,
            equalTo("/kafka/v3/clusters/foo/topics/bar/partitions/0")
        )

        assertThat(
            kafkaRest.produceRecords(
                clusterId, topic, listOf(
                    Record(RecordData.Binary(Base64Blob.encode("1")), RecordData.Binary(Base64Blob.encode("3"))),
                    Record(RecordData.Binary(Base64Blob.encode("2")), RecordData.Binary(Base64Blob.encode("4")))
                )
            ).successValue()!!.toList().size, equalTo(2)
        )

        val group = ConsumerGroup.of(org.http4k.connect.kafka.rest.randomString())

        val consumer1 = KafkaRestConsumer.Http(
            Credentials("", ""), group,
            Consumer(
                ConsumerInstance.of("--1"),
                RecordFormat.binary,
                AutoOffsetReset.earliest, enableAutocommit = AutoCommitEnable.`false`
            ), uri, http
        ).successValue()

        consumer1.subscribeToTopics(listOf(topic)).successValue()

        val records = consumer1.consumeRecordsTwiceBecauseOfProxy(RecordFormat.json)
        assertThat(records
            .map { it.key.toString() to it.value.toString() }
            .map { Base64Blob.of(it.first).decoded() to Base64Blob.of(it.second).decoded() },
            equalTo(listOf("1" to "3", "2" to "4"))
        )

        val kafkaTopicList = kafkaRest.getTopics(clusterId).successValue()!!
        assertThat(kafkaTopicList.metadata.self.path, equalTo("/kafka/v3/clusters/foo/topics"))
        assertThat(kafkaTopicList.data.first().topic_name, equalTo(topic))
        assertThat(kafkaTopicList.data.first().cluster_id, equalTo(clusterId))
    }

    @Test
    fun `send and receive JSON records`() {
        val clusterId = ClusterId.of("foo")
        val topic = Topic.of("bar")
        val kafkaTopic = kafkaRest.getTopic(clusterId, topic).successValue()
        assertThat(kafkaTopic?.cluster_id, equalTo(clusterId))
        assertThat(kafkaTopic?.topic_name, equalTo(topic))
        assertThat(kafkaTopic?.metadata?.self?.path, equalTo("/kafka/v3/clusters/foo/topics/bar"))

        val partitionList = kafkaRest.getPartitions(clusterId, topic).successValue()
        assertThat(partitionList?.`data`?.first()?.cluster_id, equalTo(clusterId))
        assertThat(partitionList?.`data`?.first()?.topic_name, equalTo(topic))
        assertThat(
            partitionList?.`data`?.first()?.metadata?.self?.path,
            equalTo("/kafka/v3/clusters/foo/topics/bar/partitions/0")
        )

        assertThat(
            kafkaRest.produceRecords(
                clusterId, topic, listOf(
                    Record(RecordData.Json("foo1"), RecordData.Json(mapOf("bar1" to "foo1"))),
                )
            ).successValue()!!.toList().size, equalTo(1)
        )

        val group = ConsumerGroup.of(org.http4k.connect.kafka.rest.randomString())

        val consumer1 = KafkaRestConsumer.Http(
            Credentials("", ""), group,
            Consumer(
                ConsumerInstance.of("--1"),
                RecordFormat.binary,
                AutoOffsetReset.earliest, enableAutocommit = AutoCommitEnable.`false`
            ), uri, http
        ).successValue()

        consumer1.subscribeToTopics(listOf(topic)).successValue()

        val records = consumer1.consumeRecordsTwiceBecauseOfProxy(RecordFormat.json)
        assertThat(
            records
                .map { it.key to it.value },
            equalTo(
                listOf(
                    "foo1" to mapOf("bar1" to "foo1")
                )
            )
        )

        val kafkaTopicList = kafkaRest.getTopics(clusterId).successValue()!!
        assertThat(kafkaTopicList.metadata.self.path, equalTo("/kafka/v3/clusters/foo/topics"))
        assertThat(kafkaTopicList.data.first().topic_name, equalTo(topic))
        assertThat(kafkaTopicList.data.first().cluster_id, equalTo(clusterId))
    }
}
