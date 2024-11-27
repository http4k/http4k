package org.http4k.connect.amazon.sns

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.DataType
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.sns.action.PublishBatchResultEntry
import org.http4k.connect.amazon.sns.model.MessageAttribute
import org.http4k.connect.amazon.sns.model.SNSMessageId
import org.http4k.connect.amazon.sns.model.TopicName
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.body.form
import org.http4k.core.body.formAsMap
import org.http4k.core.with
import org.http4k.routing.asRouter
import org.http4k.routing.bind
import org.http4k.template.PebbleTemplates
import org.http4k.template.viewModel
import java.util.UUID

fun createTopic(topics: Storage<List<SNSMessage>>, awsAccount: AwsAccount, region: Region) =
    { r: Request -> r.form("Action") == "CreateTopic" }
        .asRouter() bind { req: Request ->
        val topicName = TopicName.of(req.form("Name")!!)
        if (topics.keySet(topicName.value).isEmpty()) topics[topicName.value] = listOf()

        Response(OK).with(
            viewModelLens of CreateTopicResponse(
                ARN.of(SNS.awsService, region, awsAccount, topicName)
            )
        )
    }

fun deleteTopic(topics: Storage<List<SNSMessage>>, awsAccount: AwsAccount, region: Region) =
    { r: Request -> r.form("Action") == "DeleteTopic" }
        .asRouter() bind { req: Request ->
        val topicName = ARN.parse(req.form("TopicArn")!!).resourceId(TopicName::of)

        when {
            topics.keySet(topicName.value)
                .isEmpty() -> Response(BAD_REQUEST).body("cannot find topic $topicName in $region/$awsAccount. Existing: ${topics.keySet()}")

            else -> {
                topics.remove(topicName.value)
                Response(OK).with(viewModelLens of DeleteTopicResponse)
            }
        }
    }

fun listTopics(topics: Storage<List<SNSMessage>>, awsAccount: AwsAccount, region: Region) =
    { r: Request -> r.form("Action") == "ListTopics" }
        .asRouter() bind {
        Response(OK).with(
            viewModelLens of ListTopicsResponse(
                topics.keySet("").map { ARN.of(SNS.awsService, region, awsAccount, it) })
        )
    }

fun publish(topics: Storage<List<SNSMessage>>, awsAccount: AwsAccount, region: Region) =
    { r: Request -> r.form("Action") == "Publish" }
        .asRouter() bind { req: Request ->

        val topicName = ARN.parse(req.form("TopicArn")!!).resourceId(TopicName::of)

        when {
            topics.keySet(topicName.value)
                .isEmpty() -> Response(BAD_REQUEST).body("cannot find topic $topicName in $region/$awsAccount. Existing: ${topics.keySet()}")

            else -> {
                topics[topicName.value] = topics[topicName.value]!! + SNSMessage(
                    req.form("Message")!!,
                    req.form("Subject"),
                    attributesFrom(req)
                )
                Response(OK).with(viewModelLens of PublishResponse(SNSMessageId.of(UUID.randomUUID().toString())))
            }
        }
    }

fun publishBatch(topics: Storage<List<SNSMessage>>, awsAccount: AwsAccount, region: Region) =
    { r: Request -> r.form("Action") == "PublishBatch" }
        .asRouter() bind fn@{ req: Request ->

        val topicName = ARN.parse(req.form("TopicArn")!!).resourceId(TopicName::of)
        if (topics.keySet(topicName.value)
                .isEmpty()
        ) return@fn Response(BAD_REQUEST).body("cannot find topic $topicName in $region/$awsAccount. Existing: ${topics.keySet()}")

        val results = (1 until Int.MAX_VALUE)
            .asSequence()
            .map { index ->
                val id = req.form("PublishBatchRequestEntries.member.$index.Id") ?: return@map null

                topics[topicName.value] = topics[topicName.value]!! + SNSMessage(
                    message = req.form("PublishBatchRequestEntries.member.$index.Message")!!,
                    subject = req.form("PublishBatchRequestEntries.member.$index.Subject"),
                    attributes = attributesFrom(req, prefix = "PublishBatchRequestEntries.member.$index.")
                )

                PublishBatchResultEntry(
                    Id = id,
                    MessageId = SNSMessageId.of(UUID.randomUUID().toString()),
                    SequenceNumber = null
                )
            }
            .takeWhile { it != null }
            .filterNotNull()
            .toList()

        Response(OK).with(viewModelLens of PublishBatchResponse(results))
    }

val viewModelLens by lazy {
    Body.viewModel(PebbleTemplates().CachingClasspath(), APPLICATION_XML).toLens()
}

private fun attributesFrom(req: Request, prefix: String = ""): List<MessageAttribute> {
    val names = req.formAsMap()
        .filter { it.key.startsWith("${prefix}MessageAttributes") }
        .filter { it.key.endsWith(".Name") }
        .map {
            it.value.first()!!.removePrefix("[").removeSuffix("]") to
                it.key.removePrefix("${prefix}MessageAttributes.entry.").removeSuffix(".Name")
        }

    val cleanedValues = req.formAsMap().mapKeys {
        it.key
            .removeSuffix(".StringValue")
            .removeSuffix(".BinaryValue")
    }

    return names.map {
        MessageAttribute(
            it.first,
            cleanedValues["${prefix}MessageAttributes.entry.${it.second}.Value"]
            !!.toString().removePrefix("[").removeSuffix("]"),
            DataType.valueOf(cleanedValues["${prefix}MessageAttributes.entry.${it.second}.Value.DataType"]!![0]!!)
        )
    }
}
