package org.http4k.connect.amazon.secretsmanager

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class SecretsManagerAction<R : Any>(clazz: KClass<R>, autoMarshalling: AutoMarshalling = SecretsManagerMoshi) :
    AwsJsonAction<R>(SecretsManager.awsService, clazz, autoMarshalling)
