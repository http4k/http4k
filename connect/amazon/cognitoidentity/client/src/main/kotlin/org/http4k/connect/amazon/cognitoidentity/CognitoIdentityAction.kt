package org.http4k.connect.amazon.cognitoidentity

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class CognitoIdentityAction<R : Any>(
    clazz: KClass<R>,
    autoMarshalling: AutoMarshalling = CognitoIdentityMoshi,
) : AwsJsonAction<R>(AwsService.of("AWSCognitoIdentityService"), clazz, autoMarshalling)
