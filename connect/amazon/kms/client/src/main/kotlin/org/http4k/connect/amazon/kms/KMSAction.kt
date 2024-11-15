package org.http4k.connect.amazon.kms

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class KMSAction<R : Any>(clazz: KClass<R>, autoMarshalling: AutoMarshalling = KMSMoshi) :
    AwsJsonAction<R>(AwsService.of("TrentService"), clazz, autoMarshalling)
