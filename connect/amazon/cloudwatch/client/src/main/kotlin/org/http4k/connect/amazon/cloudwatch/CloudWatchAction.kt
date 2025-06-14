package org.http4k.connect.amazon.cloudwatch

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class CloudWatchAction<R : Any>(clazz: KClass<R>, autoMarshalling: AutoMarshalling = CloudWatchMoshi) :
    AwsJsonAction<R>(AwsService.of("GraniteServiceVersion20100801"), clazz, autoMarshalling)
