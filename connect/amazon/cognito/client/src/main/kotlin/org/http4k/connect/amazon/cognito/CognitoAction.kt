package org.http4k.connect.amazon.cognito

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class CognitoAction<R : Any>(clazz: KClass<R>, autoMarshalling: AutoMarshalling = CognitoMoshi) :
    AwsJsonAction<R>(AwsService.of("AWSCognitoIdentityProviderService"), clazz, autoMarshalling)
