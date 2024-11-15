package org.http4k.connect.kafka.rest.action

import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kafka.rest.KafkaRestMoshi
import kotlin.reflect.KClass

abstract class NonNullKafkaRestAction<R : Any>(clazz: KClass<R>) : NonNullAutoMarshalledAction<R>(clazz, KafkaRestMoshi)

