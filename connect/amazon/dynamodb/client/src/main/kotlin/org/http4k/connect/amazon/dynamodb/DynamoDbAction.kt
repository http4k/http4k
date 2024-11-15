package org.http4k.connect.amazon.dynamodb

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.core.ContentType
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class DynamoDbAction<R : Any>(clazz: KClass<R>, autoMarshalling: AutoMarshalling = DynamoDbMoshi) :
    AwsJsonAction<R>(
        AwsService.of("DynamoDB_20120810"),
        clazz,
        autoMarshalling,
        ContentType("application/x-amz-json-1.0")
    )
