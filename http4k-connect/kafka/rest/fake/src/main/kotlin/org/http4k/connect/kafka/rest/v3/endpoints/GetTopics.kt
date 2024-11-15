package org.http4k.connect.kafka.rest.v3.endpoints

import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.model.SendRecord
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v3.action.KafkaTopic
import org.http4k.connect.kafka.rest.v3.action.KafkaTopicList
import org.http4k.connect.kafka.rest.v3.model.Metadata
import org.http4k.connect.kafka.rest.v3.model.Relation
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.with
import org.http4k.routing.bind

fun getTopics(baseUrl: Uri, topics: Storage<List<SendRecord>>) =
    "/kafka/v3/clusters/{cluster}/topics" bind GET to {
        Response(OK)
            .with(
                Body.auto<KafkaTopicList>().toLens() of
                    KafkaTopicList(
                        topics.keySet().sorted().map { t ->
                            KafkaTopic(
                                clusterId(it),
                                Topic.of(t),
                                Metadata(baseUrl.extend(it.uri), null, null),
                                false,
                                0,
                                1,
                                Relation(it.uri),
                                Relation(it.uri),
                                Relation(it.uri),
                                emptyList()
                            )
                        },
                        Metadata(baseUrl.extend(it.uri), null, null)
                    )
            )
    }
