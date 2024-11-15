package org.http4k.connect.amazon.systemsmanager

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class SystemsManagerAction<R : Any>(clazz: KClass<R>, autoMarshalling: AutoMarshalling = SystemsManagerMoshi) :
    AwsJsonAction<R>(AwsService.of("AmazonSSM"), clazz, autoMarshalling)
