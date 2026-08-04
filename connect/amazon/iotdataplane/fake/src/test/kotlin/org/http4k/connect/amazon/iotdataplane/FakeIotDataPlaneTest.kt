package org.http4k.connect.amazon.iotdataplane

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.present
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.iotdataplane.action.RetainedMessage
import org.http4k.connect.amazon.iotdataplane.model.PayloadFormatIndicator.UTF8_DATA
import org.http4k.connect.amazon.iotdataplane.model.ShadowName
import org.http4k.connect.amazon.iotdataplane.model.ThingName
import org.http4k.connect.amazon.iotdataplane.model.TopicName
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.TimestampMillis
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset.UTC

class FakeIotDataPlaneTest : IotDataPlaneContract, FakeAwsContract {
    override val http = FakeIotDataPlane()

    override val endpoint = Uri.of("https://http4k-ats.iot.ldn-north-1.amazonaws.com")

    @Test
    fun `records the payload of a published message`() {
        val topic = TopicName.of("$topicPrefix/recorded")

        iotDataPlane.publish(topic, "hello world".toByteArray()).successValue()

        val messages = http.publishedMessages(topic)
        assertThat(messages, hasSize(equalTo(1)))
        assertThat(messages[0].topic, equalTo(topic))
        assertThat(messages[0].payload, equalTo(Base64Blob.encode("hello world")))
        assertThat(messages[0].payload.decoded(), equalTo("hello world"))
        assertThat(messages[0].qos, equalTo(null))
        assertThat(messages[0].correlationData, equalTo(null))
    }

    @Test
    fun `records every parameter and header`() {
        val topic = TopicName.of("$topicPrefix/full")

        iotDataPlane.publish(
            topic = topic,
            payload = byteArrayOf(1, 2, 3),
            qos = 1,
            retain = true,
            messageExpiry = 60,
            responseTopic = "$topicPrefix/response",
            contentType = "text/plain",
            correlationData = Base64Blob.encode("correlation"),
            payloadFormatIndicator = UTF8_DATA,
            userProperties = listOf("k1" to "v1", "k2" to "v2")
        ).successValue()

        assertThat(
            http.publishedMessages(topic), equalTo(
                listOf(
                    PublishedMessage(
                        topic = topic,
                        payload = Base64Blob.encode(byteArrayOf(1, 2, 3)),
                        qos = 1,
                        retain = true,
                        contentType = "text/plain",
                        messageExpiry = 60,
                        responseTopic = "$topicPrefix/response",
                        correlationData = Base64Blob.encode("correlation").value,
                        payloadFormatIndicator = "UTF8_DATA",
                        userProperties = Base64Blob.encode("""[{"k1":"v1"},{"k2":"v2"}]""").value
                    )
                )
            )
        )
    }

    @Test
    fun `records a slashed topic under its full decoded name`() {
        val topic = TopicName.of("$topicPrefix/a/b/c")

        iotDataPlane.publish(topic, "nested".toByteArray()).successValue()

        assertThat(http.publishedMessages(topic).map { it.topic }, equalTo(listOf(topic)))
        assertThat(http.publishedMessages(TopicName.of("$topicPrefix/a")), hasSize(equalTo(0)))
    }

    @Test
    fun `records repeated publishes to the same topic in order`() {
        val topic = TopicName.of("$topicPrefix/repeat")

        iotDataPlane.publish(topic, "one".toByteArray()).successValue()
        iotDataPlane.publish(topic, "two".toByteArray()).successValue()

        assertThat(
            http.publishedMessages(topic).map { it.payload.decoded() },
            equalTo(listOf("one", "two"))
        )
    }

    @Test
    fun `client convenience function targets the fake`() {
        val topic = TopicName.of("$topicPrefix/client")

        http.client().publish(topic, "via client".toByteArray()).successValue()

        assertThat(http.publishedMessages(topic).map { it.payload.decoded() }, equalTo(listOf("via client")))
    }

    @Test
    fun `stores the merged shadow document under the thing and shadow name`() {
        val thing = thing("stored")
        val shadowName = ShadowName.of("config")

        iotDataPlane.updateThingShadow(thing, """{"state":{"reported":{"on":true}}}""".toByteArray(), shadowName)
            .successValue()
        iotDataPlane.updateThingShadow(thing, """{"state":{"desired":{"on":false}}}""".toByteArray(), shadowName)
            .successValue()

        assertThat(
            http.shadow(thing, shadowName),
            equalTo("""{"state":{"reported":{"on":true},"desired":{"on":false}},"version":2}""")
        )
        assertThat(http.shadow(thing), equalTo(null))
    }

    @Test
    fun `removes the stored document on delete`() {
        val thing = thing("removed")

        iotDataPlane.updateThingShadow(thing, """{"state":{"reported":{"on":true}}}""".toByteArray()).successValue()
        assertThat(http.shadow(thing), present())

        iotDataPlane.deleteThingShadow(thing).successValue()
        assertThat(http.shadow(thing), equalTo(null))
    }

    @Test
    fun `answers a delete with the version of the deleted shadow`() {
        val thing = thing("deletedVersion")

        iotDataPlane.updateThingShadow(thing, """{"state":{"reported":{"on":true}}}""".toByteArray()).successValue()

        assertThat(
            iotDataPlane.deleteThingShadow(thing).successValue().reader().readText(),
            equalTo("""{"version":1}""")
        )
    }

    @Test
    fun `shadows are seeded from the storage the fake is given`() {
        val thing = ThingName.of("seeded")
        val seeded = Storage.InMemory<String>()
        seeded[storageKey(thing)] = """{"state":{"reported":{"on":true}},"version":7}"""

        val document = FakeIotDataPlane(shadows = seeded).client()
            .getThingShadow(thing)
            .successValue()
            .reader()
            .readText()

        assertThat(document, equalTo("""{"state":{"reported":{"on":true}},"version":7}"""))
    }

    @Test
    fun `records a retained publish in both the message log and the retained store`() {
        val topic = TopicName.of("$topicPrefix/retained")

        iotDataPlane.publish(topic, "kept".toByteArray(), qos = 1, retain = true).successValue()

        assertThat(http.publishedMessages(topic).map { it.payload.decoded() }, equalTo(listOf("kept")))
        assertThat(http.retainedMessage(topic)?.payload, equalTo(Base64Blob.encode("kept")))
        assertThat(http.retainedMessage(topic)?.qos, equalTo(1))
    }

    @Test
    fun `a publish without the retain flag retains nothing`() {
        val topic = TopicName.of("$topicPrefix/unretained")

        iotDataPlane.publish(topic, "transient".toByteArray()).successValue()
        iotDataPlane.publish(topic, "also transient".toByteArray(), retain = false).successValue()

        assertThat(http.publishedMessages(topic), hasSize(equalTo(2)))
        assertThat(http.retainedMessage(topic), equalTo(null))
    }

    @Test
    fun `a retained publish replaces the stored message and an empty one removes it`() {
        val topic = TopicName.of("$topicPrefix/replaced")

        iotDataPlane.publish(topic, "first".toByteArray(), retain = true).successValue()
        iotDataPlane.publish(topic, "second".toByteArray(), retain = true).successValue()

        assertThat(http.retainedMessage(topic)?.payload, equalTo(Base64Blob.encode("second")))

        iotDataPlane.publish(topic, ByteArray(0), retain = true).successValue()

        assertThat(http.retainedMessage(topic), equalTo(null))
        // clearing the retained message does not un-record the publishes
        assertThat(http.publishedMessages(topic), hasSize(equalTo(3)))
    }

    /**
     * The contract only checks the shape of a listing, because a real account's retained store is
     * unbounded and shared. The full semantics - every retained message listed, with its payload
     * size, reachable by following nextToken - are proved here, against a store which is closed.
     */
    @Test
    fun `lists every message in the retained store, a page at a time`() {
        val client = FakeIotDataPlane().client()
        val topics = listOf("one", "three", "two").map { TopicName.of("listed/$it") }

        topics.forEach { client.publish(it, "listed".toByteArray(), qos = 1, retain = true).successValue() }

        val summaries = generateSequence(client.listRetainedMessages(maxResults = 2).successValue()) { page ->
            page.nextToken?.let { client.listRetainedMessages(maxResults = 2, nextToken = it).successValue() }
        }.flatMap { it.retainedTopics }.toList()

        assertThat(summaries.map { it.topic }, equalTo(topics))
        assertThat(summaries.map { it.payloadSize }.distinct(), equalTo(listOf("listed".length.toLong())))
        assertThat(summaries.map { it.qos }.distinct(), equalTo(listOf(1)))
    }

    @Test
    fun `retained messages are seeded from the storage the fake is given and timed by its clock`() {
        val seeded = TopicName.of("seeded/topic")
        val published = TopicName.of("published/topic")
        val clock = Clock.fixed(Instant.parse("2021-02-26T15:26:33Z"), UTC)
        val store = Storage.InMemory<RetainedMessage>()
        store[seeded.value] = RetainedMessage(seeded, TimestampMillis.of(1L), 1, Base64Blob.encode("seeded"))

        val fake = FakeIotDataPlane(retainedMessages = store, clock = clock)

        fake.client().publish(published, "now".toByteArray(), retain = true).successValue()

        assertThat(fake.client().getRetainedMessage(seeded).successValue().payload?.decoded(), equalTo("seeded"))
        assertThat(
            fake.client().getRetainedMessage(published).successValue().lastModifiedTime,
            equalTo(TimestampMillis.of(clock.instant()))
        )
    }
}
