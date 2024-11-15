package org.http4k.connect.kafka.rest.v3.endpoints

import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.v3.action.KafkaTopic
import org.http4k.connect.kafka.rest.v3.model.Metadata
import org.http4k.connect.kafka.rest.v3.model.Relation
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.with
import org.http4k.routing.bind

fun getTopic(baseUrl: Uri) =
    "/kafka/v3/clusters/{cluster}/topics/{topic}" bind GET to {
        Response(OK)
            .with(
                Body.auto<KafkaTopic>().toLens() of
                    KafkaTopic(
                        clusterId(it),
                        topic(it),
                        Metadata(baseUrl.extend(it.uri), null, null),
                        false,
                        0,
                        1,
                        Relation(it.uri),
                        Relation(it.uri),
                        Relation(it.uri),
                        emptyList()
                    )
            )
    }
