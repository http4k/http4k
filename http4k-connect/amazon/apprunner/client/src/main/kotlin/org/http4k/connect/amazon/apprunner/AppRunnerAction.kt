package org.http4k.connect.amazon.apprunner

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.core.ContentType
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class AppRunnerAction<R : Any>(clazz: KClass<R>, autoMarshalling: AutoMarshalling = AppRunnerMoshi) :
    AwsJsonAction<R>(AwsService.of("AppRunner"), clazz, autoMarshalling, ContentType("application/x-amz-json-1.0"))
