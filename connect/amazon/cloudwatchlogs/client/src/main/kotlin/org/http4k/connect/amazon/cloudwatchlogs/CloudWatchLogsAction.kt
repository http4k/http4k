package org.http4k.connect.amazon.cloudwatchlogs

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class CloudWatchLogsAction<R : Any>(clazz: KClass<R>, autoMarshalling: AutoMarshalling = CloudWatchLogsMoshi) :
    AwsJsonAction<R>(AwsService.of("Logs_20140328"), clazz, autoMarshalling)
