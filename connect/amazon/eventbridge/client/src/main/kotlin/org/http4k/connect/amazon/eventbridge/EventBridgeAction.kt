package org.http4k.connect.amazon.eventbridge

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class EventBridgeAction<R : Any>(clazz: KClass<R>, autoMarshalling: AutoMarshalling = EventBridgeMoshi) :
    AwsJsonAction<R>(AwsService.of("AWSEvents"), clazz, autoMarshalling)
