package org.http4k.connect.kafka.rest.action

import org.http4k.connect.NullableAutoMarshalledAction
import org.http4k.connect.kafka.rest.KafkaRestMoshi
import kotlin.reflect.KClass

abstract class NullableKafkaRestAction<R : Any>(clazz: KClass<R>) :
    NullableAutoMarshalledAction<R>(clazz, KafkaRestMoshi)
