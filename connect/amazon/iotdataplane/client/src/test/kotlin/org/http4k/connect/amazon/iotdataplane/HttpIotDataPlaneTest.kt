package org.http4k.connect.amazon.iotdataplane

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.FakeAwsEnvironment
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iotdataplane.action.DeleteConnection
import org.http4k.connect.amazon.iotdataplane.action.GetRetainedMessage
import org.http4k.connect.amazon.iotdataplane.action.GetThingShadow
import org.http4k.connect.amazon.iotdataplane.action.Publish
import org.http4k.connect.amazon.iotdataplane.action.RetainedMessageSummary
import org.http4k.connect.amazon.iotdataplane.action.UpdateThingShadow
import org.http4k.connect.amazon.iotdataplane.model.ClientId
import org.http4k.connect.amazon.iotdataplane.model.PayloadFormatIndicator.UNSPECIFIED_BYTES
import org.http4k.connect.amazon.iotdataplane.model.PayloadFormatIndicator.UTF8_DATA
import org.http4k.connect.amazon.iotdataplane.model.ShadowName
import org.http4k.connect.amazon.iotdataplane.model.ThingName
import org.http4k.connect.amazon.iotdataplane.model.TopicName
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.Timestamp
import org.http4k.connect.model.TimestampMillis
import org.http4k.connect.successValue
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.MockHttp
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasMethod
import org.http4k.hamkrest.hasUri
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class HttpIotDataPlaneTest {

    companion object {
        private val base = Publish(
            topic = TopicName.of("topic"),
            payload = "hi".toByteArray(),
            qos = 1,
            retain = true,
            messageExpiry = 60,
            responseTopic = "response/topic",
            contentType = "text/plain",
            correlationData = Base64Blob.encode("correlation"),
            payloadFormatIndicator = UTF8_DATA,
            userProperties = listOf("k1" to "v1")
        )

        @JvmStatic
        fun publishesDifferingInOneField() = listOf(
            Arguments.of("payload", base.copy(payload = "ho".toByteArray())),
            Arguments.of("topic", base.copy(topic = TopicName.of("other"))),
            Arguments.of("qos", base.copy(qos = 0)),
            Arguments.of("retain", base.copy(retain = false)),
            Arguments.of("messageExpiry", base.copy(messageExpiry = 61)),
            Arguments.of("responseTopic", base.copy(responseTopic = "response/other")),
            Arguments.of("contentType", base.copy(contentType = "application/json")),
            Arguments.of("correlationData", base.copy(correlationData = Base64Blob.encode("other"))),
            Arguments.of("payloadFormatIndicator", base.copy(payloadFormatIndicator = UNSPECIFIED_BYTES)),
            Arguments.of("userProperties", base.copy(userProperties = listOf("k1" to "v2")))
        )
    }

    private val endpoint = Uri.of("https://abc123-ats.iot.us-east-1.amazonaws.com")

    private val thingName = ThingName.of("my-thing")

    /** An empty JSON document is what AWS answers with when there is nothing to report. */
    private val mockHttp = MockHttp(Response(OK).body("{}"))

    private val iotDataPlane = IotDataPlane.Http(
        endpoint,
        Region.US_EAST_1,
        CredentialsProvider.FakeAwsEnvironment(),
        mockHttp
    )

    @Test
    fun `publish builds the documented request`() {
        iotDataPlane.publish(
            topic = TopicName.of("a/b/c"),
            payload = "hello world".toByteArray(),
            qos = 1,
            retain = true,
            messageExpiry = 60,
            responseTopic = "response/topic",
            contentType = "text/plain",
            correlationData = Base64Blob.encode("correlation"),
            payloadFormatIndicator = UTF8_DATA,
            userProperties = listOf("k1" to "v1", "k2" to "v2")
        ).successValue()

        val request = mockHttp.request!!

        assertThat(request, hasMethod(POST))

        assertThat(
            request, hasUri(
                Uri.of(
                    "https://abc123-ats.iot.us-east-1.amazonaws.com/topics/a/b/c" +
                        "?contentType=text%2Fplain&messageExpiry=60&qos=1" +
                        "&responseTopic=response%2Ftopic&retain=true"
                )
            )
        )

        assertThat(request, hasHeader("Content-Type", "application/octet-stream"))
        assertThat(request, hasHeader("x-amz-mqtt5-correlation-data", Base64Blob.encode("correlation").value))
        assertThat(request, hasHeader("x-amz-mqtt5-payload-format-indicator", "UTF8_DATA"))
        assertThat(
            request,
            hasHeader("x-amz-mqtt5-user-properties", Base64Blob.encode("""[{"k1":"v1"},{"k2":"v2"}]""").value)
        )

        assertThat(request, hasBody("hello world"))
        assertThat(request.body.payload.array().toList(), equalTo("hello world".toByteArray().toList()))
    }

    @Test
    fun `publish omits every optional field when not set`() {
        iotDataPlane.publish(
            topic = TopicName.of("simple"),
            payload = byteArrayOf(1, 2, 3)
        ).successValue()

        val request = mockHttp.request!!

        assertThat(request, hasUri(Uri.of("https://abc123-ats.iot.us-east-1.amazonaws.com/topics/simple")))
        assertThat(request.header("x-amz-mqtt5-correlation-data"), equalTo(null))
        assertThat(request.header("x-amz-mqtt5-payload-format-indicator"), equalTo(null))
        assertThat(request.header("x-amz-mqtt5-user-properties"), equalTo(null))
        assertThat(request.body.payload.array().toList(), equalTo(listOf<Byte>(1, 2, 3)))
    }

    /** The action leaves the topic unencoded, because the AWS auth filter encodes the path later. */
    @Test
    fun `action leaves percent-encoding of the topic to the signing filter`() {
        assertThat(
            Publish(TopicName.of("\$aws/rules/rule/a b"), "hi".toByteArray()).toRequest().uri.path,
            equalTo("/topics/\$aws/rules/rule/a b")
        )

        iotDataPlane.publish(TopicName.of("\$aws/rules/rule/a b"), "hi".toByteArray()).successValue()

        assertThat(mockHttp.request!!.uri.path, equalTo("/topics/%24aws/rules/rule/a%20b"))
    }

    /** Two publishes carrying the same bytes must compare equal. */
    @Test
    fun `equality is by payload content`() {
        val same = base.copy(payload = "hi".toByteArray())

        assertThat(same, equalTo(base))
        assertThat(same.hashCode(), equalTo(base.hashCode()))
    }

    /** One variant per field, so a field dropped from the comparison fails its case. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("publishesDifferingInOneField")
    fun `publishes differing in a single field are not equal`(field: String, other: Publish) {
        assertThat(field, other, !equalTo(base))
    }

    @Test
    fun `get thing shadow builds the documented request`() {
        iotDataPlane.getThingShadow(thingName).successValue()

        assertThat(mockHttp.request!!, hasMethod(GET))
        assertThat(mockHttp.request!!, hasUri(uri("/things/my-thing/shadow")))
    }

    @Test
    fun `get named thing shadow builds the documented request`() {
        iotDataPlane.getThingShadow(thingName, ShadowName.of("config")).successValue()

        assertThat(mockHttp.request!!, hasUri(uri("/things/my-thing/shadow?name=config")))
    }

    @Test
    fun `update thing shadow builds the documented request`() {
        iotDataPlane.updateThingShadow(
            thingName,
            """{"state":{"reported":{"on":true}}}""".toByteArray(),
            ShadowName.of("config")
        ).successValue()

        val request = mockHttp.request!!

        assertThat(request, hasMethod(POST))
        assertThat(request, hasUri(uri("/things/my-thing/shadow?name=config")))
        assertThat(request, hasHeader("Content-Type", "application/octet-stream"))
        assertThat(request, hasBody("""{"state":{"reported":{"on":true}}}"""))
    }

    @Test
    fun `delete thing shadow builds the documented request`() {
        iotDataPlane.deleteThingShadow(thingName).successValue()

        assertThat(mockHttp.request!!, hasMethod(DELETE))
        assertThat(mockHttp.request!!, hasUri(uri("/things/my-thing/shadow")))
    }

    @Test
    fun `list named shadows builds the documented request`() {
        iotDataPlane.listNamedShadowsForThing(thingName, nextToken = "token", pageSize = 2).successValue()

        assertThat(mockHttp.request!!, hasMethod(GET))
        assertThat(
            mockHttp.request!!,
            hasUri(uri("/api/things/shadow/ListNamedShadowsForThing/my-thing?nextToken=token&pageSize=2"))
        )
    }

    @Test
    fun `list named shadows omits both optional query parameters when not set`() {
        iotDataPlane.listNamedShadowsForThing(thingName).successValue()

        assertThat(mockHttp.request!!, hasUri(uri("/api/things/shadow/ListNamedShadowsForThing/my-thing")))
    }

    @Test
    fun `list named shadows unmarshals the response document`() {
        val listing = IotDataPlane.Http(
            endpoint,
            Region.US_EAST_1,
            CredentialsProvider.FakeAwsEnvironment(),
            MockHttp(Response(OK).body("""{"results":["alpha","beta"],"nextToken":"token","timestamp":1614355593}"""))
        ).listNamedShadowsForThing(thingName).successValue()

        assertThat(listing.results, equalTo(listOf(ShadowName.of("alpha"), ShadowName.of("beta"))))
        assertThat(listing.nextToken, equalTo("token"))
        assertThat(listing.timestamp, equalTo(Timestamp.of(1614355593)))
    }

    /** A colon is the one character the thing-name charset allows which a path must still escape. */
    @Test
    fun `action leaves percent-encoding of the thing name to the signing filter`() {
        assertThat(
            GetThingShadow(ThingName.of("thing:one")).toRequest().uri.path,
            equalTo("/things/thing:one/shadow")
        )

        iotDataPlane.getThingShadow(ThingName.of("thing:one")).successValue()

        assertThat(mockHttp.request!!.uri.path, equalTo("/things/thing%3Aone/shadow"))
    }

    /** A slashed or spaced name would build a path AWS rejects and the fake cannot route. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["", " ", "thing one", "thing/one", "thing.one", "thing+one"])
    fun `thing and shadow names outside the AWS charset are rejected`(name: String) {
        assertThrows<IllegalArgumentException> { ThingName.of(name) }
        assertThrows<IllegalArgumentException> { ShadowName.of(name) }
    }

    @Test
    fun `thing and shadow names over the AWS length limit are rejected`() {
        ThingName.of("t".repeat(128))
        ShadowName.of("s".repeat(64))

        assertThrows<IllegalArgumentException> { ThingName.of("t".repeat(129)) }
        assertThrows<IllegalArgumentException> { ShadowName.of("s".repeat(65)) }
    }

    /** "The client ID can't start with a dollar sign ($)" - and nothing else is constrained. */
    @Test
    fun `client ids starting with a dollar sign are rejected`() {
        ClientId.of("client\$id")

        assertThrows<IllegalArgumentException> { ClientId.of("") }
        assertThrows<IllegalArgumentException> { ClientId.of("\$aws-client") }
    }

    /** Two updates carrying the same bytes must compare equal. */
    @Test
    fun `update equality is by payload content`() {
        val update = UpdateThingShadow(thingName, "hi".toByteArray(), ShadowName.of("config"))

        assertThat(update.copy(payload = "hi".toByteArray()), equalTo(update))
        assertThat(update.copy(payload = "hi".toByteArray()).hashCode(), equalTo(update.hashCode()))
        assertThat(update.copy(payload = "ho".toByteArray()), !equalTo(update))
        assertThat(update.copy(thingName = ThingName.of("other")), !equalTo(update))
        assertThat(update.copy(shadowName = null), !equalTo(update))
    }

    @Test
    fun `get retained message builds the documented request`() {
        val mock = MockHttp(Response(OK).body(RETAINED_MESSAGE))

        clientFor(mock).getRetainedMessage(TopicName.of("a/b")).successValue()

        assertThat(mock.request!!, hasMethod(GET))
        assertThat(mock.request!!, hasUri(uri("/retainedMessage/a/b")))
    }

    @Test
    fun `get retained message unmarshals the response document`() {
        val retained = clientFor(MockHttp(Response(OK).body(RETAINED_MESSAGE)))
            .getRetainedMessage(TopicName.of("a/b"))
            .successValue()

        assertThat(retained.topic, equalTo(TopicName.of("a/b")))
        assertThat(retained.lastModifiedTime, equalTo(TimestampMillis.of(1614355593000)))
        assertThat(retained.qos, equalTo(1))
        assertThat(retained.payload?.decoded(), equalTo("hello world"))
        assertThat(retained.userProperties?.decoded(), equalTo("""[{"deviceName":"alpha"},{"deviceCnt":"45"}]"""))
    }

    @Test
    fun `action leaves percent-encoding of the retained topic to the signing filter`() {
        assertThat(
            GetRetainedMessage(TopicName.of("\$aws/rules/rule/a b")).toRequest().uri.path,
            equalTo("/retainedMessage/\$aws/rules/rule/a b")
        )

        val mock = MockHttp(Response(OK).body(RETAINED_MESSAGE))

        clientFor(mock).getRetainedMessage(TopicName.of("\$aws/rules/rule/a b")).successValue()

        assertThat(mock.request!!.uri.path, equalTo("/retainedMessage/%24aws/rules/rule/a%20b"))
    }

    @Test
    fun `list retained messages builds the documented request`() {
        iotDataPlane.listRetainedMessages(maxResults = 2, nextToken = "token").successValue()

        assertThat(mockHttp.request!!, hasMethod(GET))
        assertThat(mockHttp.request!!, hasUri(uri("/retainedMessage?maxResults=2&nextToken=token")))
    }

    @Test
    fun `list retained messages omits both optional query parameters when not set`() {
        iotDataPlane.listRetainedMessages().successValue()

        assertThat(mockHttp.request!!, hasUri(uri("/retainedMessage")))
    }

    @Test
    fun `list retained messages unmarshals the response document`() {
        val listing = clientFor(
            MockHttp(
                Response(OK).body(
                    """{"retainedTopics":[{"topic":"a/b","lastModifiedTime":1614355593000,""" +
                        """"payloadSize":11,"qos":1}],"nextToken":"token"}"""
                )
            )
        ).listRetainedMessages().successValue()

        assertThat(listing.nextToken, equalTo("token"))
        assertThat(
            listing.retainedTopics, equalTo(
                listOf(
                    RetainedMessageSummary(
                        topic = TopicName.of("a/b"),
                        lastModifiedTime = TimestampMillis.of(1614355593000),
                        payloadSize = 11,
                        qos = 1
                    )
                )
            )
        )
    }

    @Test
    fun `delete connection builds the documented request`() {
        iotDataPlane.deleteConnection(
            clientId = ClientId.of("my-client"),
            cleanSession = true,
            preventWillMessage = false
        ).successValue()

        assertThat(mockHttp.request!!, hasMethod(DELETE))
        assertThat(
            mockHttp.request!!,
            hasUri(uri("/connections/my-client?cleanSession=true&preventWillMessage=false"))
        )
    }

    @Test
    fun `delete connection omits both optional query parameters when not set`() {
        iotDataPlane.deleteConnection(ClientId.of("my-client")).successValue()

        assertThat(mockHttp.request!!, hasUri(uri("/connections/my-client")))
    }

    @Test
    fun `action leaves percent-encoding of the client id to the signing filter`() {
        assertThat(
            DeleteConnection(ClientId.of("client:one")).toRequest().uri.path,
            equalTo("/connections/client:one")
        )

        iotDataPlane.deleteConnection(ClientId.of("client:one")).successValue()

        assertThat(mockHttp.request!!.uri.path, equalTo("/connections/client%3Aone"))
    }

    private fun clientFor(http: MockHttp) =
        IotDataPlane.Http(endpoint, Region.US_EAST_1, CredentialsProvider.FakeAwsEnvironment(), http)

    private fun uri(path: String) = Uri.of("$endpoint$path")
}

private val RETAINED_MESSAGE = """{"topic":"a/b","lastModifiedTime":1614355593000,"qos":1,""" +
    """"payload":"${Base64Blob.encode("hello world").value}",""" +
    """"userProperties":"${Base64Blob.encode("""[{"deviceName":"alpha"},{"deviceCnt":"45"}]""").value}"}"""
