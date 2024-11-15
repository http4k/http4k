package org.http4k.connect.kafka.rest.v3.endpoints

import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v3.model.ClusterId
import org.http4k.lens.Path
import org.http4k.lens.value

val clusterId = Path.value(ClusterId).of("cluster")
val topic = Path.value(Topic).of("topic")
