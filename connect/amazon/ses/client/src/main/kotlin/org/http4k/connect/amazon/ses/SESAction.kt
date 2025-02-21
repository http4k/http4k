package org.http4k.connect.amazon.ses

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.core.ContentType
import org.http4k.core.Uri
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class SESAction<R: Any>(
    clazz: KClass<R>,
    private val uri: Uri,
    autoMarshalling: AutoMarshalling = SESMoshi
) : AwsJsonAction<R>(
    SES.awsService,
    clazz,
    autoMarshalling,
    ContentType("application/x-amz-json-1.0")
) {
    override fun uri() = uri
}
