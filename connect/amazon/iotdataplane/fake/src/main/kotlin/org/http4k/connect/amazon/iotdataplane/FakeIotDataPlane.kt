package org.http4k.connect.amazon.iotdataplane

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iotdataplane.action.NamedShadows
import org.http4k.connect.amazon.iotdataplane.action.RetainedMessage
import org.http4k.connect.amazon.iotdataplane.action.RetainedMessages
import org.http4k.connect.amazon.iotdataplane.model.ShadowName
import org.http4k.connect.amazon.iotdataplane.model.ThingName
import org.http4k.connect.amazon.iotdataplane.model.TopicName
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.time.Clock

class FakeIotDataPlane(
    private val messages: Storage<List<PublishedMessage>> = Storage.InMemory(),
    private val shadows: Storage<String> = Storage.InMemory(),
    private val retainedMessages: Storage<RetainedMessage> = Storage.InMemory(),
    private val region: Region = Region.of("ldn-north-1"),
    private val endpoint: Uri = Uri.of("https://http4k-ats.iot.$region.amazonaws.com"),
    private val clock: Clock = Clock.systemUTC()
) : ChaoticHttpHandler() {

    // {topic:.+} because a topic may contain slashes
    override val app = routes(
        "/topics/{topic:.+}" bind POST to ::publish,
        "/api/things/shadow/ListNamedShadowsForThing/{thingName}" bind GET to ::listNamedShadowsForThing,
        "/things/{thingName}/shadow" bind GET to ::getThingShadow,
        "/things/{thingName}/shadow" bind POST to ::updateThingShadow,
        "/things/{thingName}/shadow" bind DELETE to ::deleteThingShadow,
        "/retainedMessage" bind GET to ::listRetainedMessages,
        "/retainedMessage/{topic:.+}" bind GET to ::getRetainedMessage,
        "/connections/{clientId}" bind DELETE to ::deleteConnection
    )

    /** Nothing is ever connected to the fake, so there is never a connection to delete. */
    private fun deleteConnection(request: Request) = connectionNotFound(request.clientId())

    private fun publish(request: Request): Response {
        val topic = TopicName.of(request.path("topic")!!)
        val payload = Base64Blob.encode(request.body.stream)

        messages[topic.value] = messages[topic.value].orEmpty() + PublishedMessage(
            topic = topic,
            payload = payload,
            qos = request.query("qos")?.toInt(),
            retain = request.query("retain")?.toBoolean(),
            contentType = request.query("contentType"),
            messageExpiry = request.query("messageExpiry")?.toLong(),
            responseTopic = request.query("responseTopic"),
            correlationData = request.header("x-amz-mqtt5-correlation-data"),
            payloadFormatIndicator = request.header("x-amz-mqtt5-payload-format-indicator"),
            userProperties = request.header("x-amz-mqtt5-user-properties")
        )

        if (request.query("retain")?.toBoolean() == true) {
            retainedMessages.retain(topic, payload, request, clock)
        }

        return Response(OK).body("{}")
    }

    private fun getThingShadow(request: Request) = shadows[request.storageKey()]
        ?.let { Response(OK).body(it) }
        ?: request.shadowNotFound()

    private fun updateThingShadow(request: Request): Response {
        val key = request.storageKey()
        val delta = IotDataPlaneMoshi.parse(request.bodyString())
        val updated = updatedShadow(shadows[key]?.let(IotDataPlaneMoshi::parse), delta)

        shadows[key] = IotDataPlaneMoshi.compact(updated)

        return Response(OK).body(IotDataPlaneMoshi.compact(acceptedShadow(delta, updated.version())))
    }

    private fun deleteThingShadow(request: Request): Response {
        val key = request.storageKey()
        val existing = shadows[key] ?: return request.shadowNotFound()

        shadows.remove(key)

        return Response(OK)
            .body(IotDataPlaneMoshi.compact(deletedShadow(IotDataPlaneMoshi.parse(existing).version())))
    }

    private fun listNamedShadowsForThing(request: Request): Response {
        val names = namedShadowsOf(request.thingName())
        val from = request.query("nextToken")?.toInt() ?: 0
        val page = names.drop(from).take(request.query("pageSize")?.toInt() ?: names.size)

        return Response(OK).body(
            IotDataPlaneMoshi.asFormatString(
                NamedShadows(
                    results = page.map(ShadowName::of),
                    nextToken = (from + page.size).takeIf { it < names.size }?.toString()
                )
            )
        )
    }

    private fun getRetainedMessage(request: Request): Response {
        val topic = TopicName.of(request.path("topic")!!)

        return retainedMessages[topic.value]
            ?.let { Response(OK).body(IotDataPlaneMoshi.asFormatString(it)) }
            ?: retainedMessageNotFound(topic)
    }

    private fun listRetainedMessages(request: Request): Response {
        val topics = retainedMessages.keySet().sorted()
        val from = request.query("nextToken")?.toInt() ?: 0
        val page = topics.drop(from).take(request.query("maxResults")?.toInt() ?: topics.size)

        return Response(OK).body(
            IotDataPlaneMoshi.asFormatString(
                RetainedMessages(
                    retainedTopics = page.mapNotNull { retainedMessages[it]?.summary() },
                    nextToken = (from + page.size).takeIf { it < topics.size }?.toString()
                )
            )
        )
    }

    private fun namedShadowsOf(thingName: ThingName) = shadows
        .keySet(storageKey(thingName))
        .map { it.substringAfter("/") }
        .filter(String::isNotEmpty)
        .sorted()

    /**
     * Convenience function to get an IotDataPlane client
     */
    fun client() = IotDataPlane.Http(endpoint, region, { AwsCredentials("accessKey", "secret") }, this)

    fun publishedMessages(topic: TopicName) = messages[topic.value].orEmpty()

    fun shadow(thingName: ThingName, shadowName: ShadowName? = null) = shadows[storageKey(thingName, shadowName)]

    fun retainedMessage(topic: TopicName) = retainedMessages[topic.value]
}

fun main() {
    FakeIotDataPlane().start()
}
