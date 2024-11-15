package org.http4k.connect.kafka.rest

import org.http4k.connect.kafka.rest.v2.KafkaRestV2Contract
import org.http4k.connect.kafka.rest.v3.KafkaRestV3Contract
import org.http4k.core.Uri

class FakeKafkaRestTest : KafkaRestV2Contract, KafkaRestV3Contract {
    override val http = FakeKafkaRest()
    override val uri = Uri.of("http://proxy")
}
