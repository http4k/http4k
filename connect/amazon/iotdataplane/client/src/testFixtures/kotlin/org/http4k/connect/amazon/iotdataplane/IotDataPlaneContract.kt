package org.http4k.connect.amazon.iotdataplane

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.iotdataplane.action.RetainedMessages
import org.http4k.connect.amazon.iotdataplane.model.ClientId
import org.http4k.connect.amazon.iotdataplane.model.PayloadFormatIndicator.UTF8_DATA
import org.http4k.connect.amazon.iotdataplane.model.ShadowName
import org.http4k.connect.amazon.iotdataplane.model.ThingName
import org.http4k.connect.amazon.iotdataplane.model.TopicName
import org.http4k.connect.failureValue
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.successValue
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.format.MoshiNode
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.concurrent.TimeUnit.SECONDS

interface IotDataPlaneContract : AwsContract {
    /** The account-specific endpoint, eg. https://xxxxxxxx-ats.iot.<region>.amazonaws.com */
    val endpoint: Uri

    val iotDataPlane get() = IotDataPlane.Http(endpoint, aws.region, { aws.credentials }, http)

    val topicPrefix get() = "http4k/${uuid()}"

    /** Shadows outlive the request which created them, so every case gets its own thing. */
    fun thing(name: String) = ThingName.of("http4k-${uuid()}-$name")

    @Test
    fun `publish message to topic`() {
        iotDataPlane.publish(
            topic = TopicName.of("$topicPrefix/simple"),
            payload = "hello world".toByteArray()
        ).successValue()
    }

    @Test
    fun `publish with all options`() {
        iotDataPlane.publish(
            topic = TopicName.of("$topicPrefix/options"),
            payload = "hello world".toByteArray(),
            qos = 1,
            retain = false,
            messageExpiry = 60,
            responseTopic = "$topicPrefix/response",
            contentType = "text/plain",
            correlationData = Base64Blob.encode("correlation"),
            payloadFormatIndicator = UTF8_DATA,
            userProperties = listOf("k1" to "v1", "k2" to "v2")
        ).successValue()
    }

    /** Proves AWS accepts a topic containing slashes sent as plain path segments, as `Publish` sends it. */
    @Test
    fun `publish to topic containing slashes`() {
        iotDataPlane.publish(
            topic = TopicName.of("$topicPrefix/a/b/c"),
            payload = "nested".toByteArray()
        ).successValue()
    }

    @Test
    fun `update then get round-trips the shadow state`() {
        val thing = thing("roundtrip")

        try {
            val accepted = iotDataPlane
                .updateThingShadow(thing, """{"state":{"reported":{"level":3,"on":true}}}""".toByteArray())
                .successValue()
                .asShadowDocument()

            assertThat(accepted.at("version"), equalTo("1"))

            val stored = iotDataPlane.getThingShadow(thing).successValue().asShadowDocument()

            assertThat(stored.at("state", "reported", "level"), equalTo("3"))
            assertThat(stored.at("state", "reported", "on"), equalTo("true"))
            assertThat(stored.at("version"), equalTo("1"))
        } finally {
            iotDataPlane.deleteShadow(thing)
        }
    }

    @Test
    fun `update merges into the stored state`() {
        val thing = thing("merge")

        try {
            iotDataPlane
                .updateThingShadow(thing, """{"state":{"reported":{"on":true,"nested":{"a":1}}}}""".toByteArray())
                .successValue()
            iotDataPlane
                .updateThingShadow(thing, """{"state":{"reported":{"nested":{"b":2}}}}""".toByteArray())
                .successValue()
            iotDataPlane
                .updateThingShadow(thing, """{"state":{"reported":{"on":null}}}""".toByteArray())
                .successValue()

            val stored = iotDataPlane.getThingShadow(thing).successValue().asShadowDocument()

            assertThat(stored.at("state", "reported", "nested", "a"), equalTo("1"))
            assertThat(stored.at("state", "reported", "nested", "b"), equalTo("2"))
            assertThat(stored.at("state", "reported", "on"), equalTo(null))
            assertThat(stored.at("version"), equalTo("3"))
        } finally {
            iotDataPlane.deleteShadow(thing)
        }
    }

    @Test
    fun `named shadows are independent of the classic shadow`() {
        val thing = thing("named")
        val shadowName = ShadowName.of("config")

        try {
            iotDataPlane.updateThingShadow(thing, """{"state":{"reported":{"which":"classic"}}}""".toByteArray())
                .successValue()
            iotDataPlane
                .updateThingShadow(thing, """{"state":{"reported":{"which":"named"}}}""".toByteArray(), shadowName)
                .successValue()

            val classic = iotDataPlane.getThingShadow(thing).successValue().asShadowDocument()
            val named = iotDataPlane.getThingShadow(thing, shadowName).successValue().asShadowDocument()

            assertThat(classic.at("state", "reported", "which"), equalTo(""""classic""""))
            assertThat(named.at("state", "reported", "which"), equalTo(""""named""""))
        } finally {
            iotDataPlane.deleteShadow(thing)
            iotDataPlane.deleteShadow(thing, shadowName)
        }
    }

    @Test
    fun `list named shadows`() {
        val thing = thing("list")
        val names = listOf("alpha", "beta").map(ShadowName::of)

        try {
            iotDataPlane.updateThingShadow(thing, SOME_STATE).successValue()
            names.forEach { iotDataPlane.updateThingShadow(thing, SOME_STATE, it).successValue() }

            assertThat(iotDataPlane.listNamedShadowsForThing(thing).successValue().results.sortedBy { it.value },
                equalTo(names))
        } finally {
            iotDataPlane.deleteShadow(thing)
            names.forEach { iotDataPlane.deleteShadow(thing, it) }
        }
    }

    @Test
    fun `list named shadows a page at a time`() {
        val thing = thing("paged")
        val names = listOf("one", "three", "two").map(ShadowName::of)

        try {
            names.forEach { iotDataPlane.updateThingShadow(thing, SOME_STATE, it).successValue() }

            val first = iotDataPlane.listNamedShadowsForThing(thing, pageSize = 2).successValue()

            assertThat(first.results.size, equalTo(2))
            assertThat(first.nextToken != null, equalTo(true))

            val second = iotDataPlane.listNamedShadowsForThing(thing, first.nextToken, pageSize = 2).successValue()

            assertThat((first.results + second.results).sortedBy { it.value }, equalTo(names.sortedBy { it.value }))
            assertThat(second.nextToken, equalTo(null))
        } finally {
            names.forEach { iotDataPlane.deleteShadow(thing, it) }
        }
    }

    @Test
    fun `get a shadow which does not exist`() {
        assertThat(iotDataPlane.getThingShadow(thing("missing")).failureValue().status, equalTo(NOT_FOUND))
    }

    @Test
    fun `delete a shadow then get it`() {
        val thing = thing("deleted")

        iotDataPlane.updateThingShadow(thing, SOME_STATE).successValue()
        iotDataPlane.deleteThingShadow(thing).successValue()

        assertThat(iotDataPlane.getThingShadow(thing).failureValue().status, equalTo(NOT_FOUND))
        assertThat(iotDataPlane.deleteThingShadow(thing).failureValue().status, equalTo(NOT_FOUND))
    }

    @Test
    fun `publish a retained message then get it`() {
        val topic = TopicName.of("$topicPrefix/retained")

        try {
            iotDataPlane.publish(
                topic = topic,
                payload = "retained payload".toByteArray(),
                qos = 1,
                retain = true,
                userProperties = listOf("deviceName" to "alpha")
            ).successValue()

            val retained = iotDataPlane.getRetainedMessage(topic).successValue()

            assertThat(retained.topic, equalTo(topic))
            assertThat(retained.payload?.decoded(), equalTo("retained payload"))
            assertThat(retained.qos, equalTo(1))
            assertThat(retained.lastModifiedTime.value > 0, equalTo(true))

            // content only - AWS is free to re-serialize the JSON it echoes back
            val userProperties = retained.userProperties?.decoded().orEmpty()
            assertThat(userProperties.contains("deviceName") && userProperties.contains("alpha"), equalTo(true))
        } finally {
            iotDataPlane.clearRetainedMessage(topic)
        }
    }

    @Test
    fun `publish a retained message to a topic containing slashes then get it`() {
        val topic = TopicName.of("$topicPrefix/a/b/c")

        try {
            iotDataPlane.publish(topic, "nested".toByteArray(), retain = true).successValue()

            assertThat(iotDataPlane.getRetainedMessage(topic).successValue().payload?.decoded(), equalTo("nested"))
        } finally {
            iotDataPlane.clearRetainedMessage(topic)
        }
    }

    @Test
    fun `a second retained publish replaces the first`() {
        val topic = TopicName.of("$topicPrefix/replaced")

        try {
            iotDataPlane.publish(topic, "first".toByteArray(), retain = true).successValue()
            iotDataPlane.publish(topic, "second".toByteArray(), retain = true).successValue()

            assertThat(iotDataPlane.getRetainedMessage(topic).successValue().payload?.decoded(), equalTo("second"))
        } finally {
            iotDataPlane.clearRetainedMessage(topic)
        }
    }

    @Test
    fun `a retained publish with an empty payload clears the retained message`() {
        val topic = TopicName.of("$topicPrefix/cleared")

        iotDataPlane.publish(topic, "value".toByteArray(), retain = true).successValue()
        iotDataPlane.getRetainedMessage(topic).successValue()

        iotDataPlane.publish(topic, ByteArray(0), retain = true).successValue()

        assertThat(iotDataPlane.getRetainedMessage(topic).failureValue().status, equalTo(NOT_FOUND))
    }

    /**
     * The retained store is account-wide and unbounded, so this proves content by point read and
     * only checks the listing's structure. FakeIotDataPlaneTest asserts the listing's contents.
     */
    @Test
    fun `list retained messages`() {
        val topics = listOf("one", "two").map { TopicName.of("$topicPrefix/list/$it") }

        try {
            topics.forEach { iotDataPlane.publish(it, "listed".toByteArray(), qos = 1, retain = true).successValue() }

            topics.forEach { topic ->
                val retained = iotDataPlane.getRetainedMessage(topic).successValue()

                assertThat(retained.topic, equalTo(topic))
                assertThat(retained.payload?.decoded(), equalTo("listed"))
                assertThat(retained.qos, equalTo(1))
                assertThat(retained.lastModifiedTime.value > 0, equalTo(true))
            }

            val page = iotDataPlane.listRetainedMessages().successValue()

            assertThat(page.retainedTopics.all { it.lastModifiedTime.value > 0 }, equalTo(true))
        } finally {
            topics.forEach(iotDataPlane::clearRetainedMessage)
        }
    }

    /**
     * maxResults is a maximum, not an exact page size. Pagination is proved by following one token
     * rather than walking to the last page, which an account-wide store does not bound.
     */
    @Test
    fun `list retained messages a page at a time`() {
        val topics = listOf("one", "two").map { TopicName.of("$topicPrefix/paged/$it") }

        try {
            topics.forEach { iotDataPlane.publish(it, "paged".toByteArray(), retain = true).successValue() }

            val first = iotDataPlane.awaitPaginableRetainedMessages()

            assertThat(first.retainedTopics.size <= 1, equalTo(true))

            assumeTrue(first.nextToken != null, "the account holds too few retained messages to paginate")

            val second = iotDataPlane.listRetainedMessages(maxResults = 1, nextToken = first.nextToken).successValue()

            assertThat(second.retainedTopics.size <= 1, equalTo(true))
        } finally {
            topics.forEach(iotDataPlane::clearRetainedMessage)
        }
    }

    @Test
    fun `get a retained message for a topic with nothing retained`() {
        val topic = TopicName.of("$topicPrefix/nothing-retained")

        assertThat(iotDataPlane.getRetainedMessage(topic).failureValue().status, equalTo(NOT_FOUND))
    }

    /**
     * A freshly named client is connected to nothing, in a real account as in the fake, so both
     * answer with what the AWS SDK's deserializer maps to ResourceNotFoundException. Disconnecting
     * a live client cannot be asserted without one, so the failure path is what the contract holds
     * both implementations to.
     */
    @Test
    fun `delete a connection which does not exist`() {
        val clientId = ClientId.of("http4k-${uuid()}")

        assertThat(iotDataPlane.deleteConnection(clientId).failureValue().status, equalTo(NOT_FOUND))
    }
}

private val SOME_STATE = """{"state":{"reported":{"created":true}}}""".toByteArray()

/**
 * The result is ignored on purpose. This runs in a finally block, so a failure here would hide the
 * assertion failure that got us there.
 */
private fun IotDataPlane.deleteShadow(thingName: ThingName, shadowName: ShadowName? = null) {
    deleteThingShadow(thingName, shadowName)
}

/**
 * Publishing an empty payload with retain=true is how AWS deletes a retained message. The result is
 * ignored for the same reason as [deleteShadow].
 */
private fun IotDataPlane.clearRetainedMessage(topic: TopicName) {
    publish(topic, ByteArray(0), retain = true)
}

/**
 * Retried, because AWS does not promise that a message which was just retained appears in this
 * listing straight away.
 */
private fun IotDataPlane.awaitPaginableRetainedMessages(): RetainedMessages {
    val deadline = System.currentTimeMillis() + SECONDS.toMillis(20)
    var page = listRetainedMessages(maxResults = 1).successValue()

    while (page.nextToken == null && System.currentTimeMillis() < deadline) {
        Thread.sleep(SECONDS.toMillis(1))
        page = listRetainedMessages(maxResults = 1).successValue()
    }

    return page
}

private fun InputStream.asShadowDocument() = IotDataPlaneMoshi.parse(reader().readText())

/** Comparing leaves keeps assertions independent of attribute order and the sections only AWS returns. */
private fun MoshiNode.at(vararg path: String) = path
    .fold<String, MoshiNode?>(this) { node, name -> node?.let { IotDataPlaneMoshi.fields(it).toMap()[name] } }
    ?.let(IotDataPlaneMoshi::compact)
