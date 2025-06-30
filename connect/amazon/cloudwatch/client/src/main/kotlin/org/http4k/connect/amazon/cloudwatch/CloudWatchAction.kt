package org.http4k.connect.amazon.cloudwatch

import org.http4k.connect.amazon.AwsJsonAction
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.core.ContentType
import org.http4k.format.AutoMarshalling
import org.http4k.lens.accept
import kotlin.reflect.KClass

abstract class CloudWatchAction<R : Any>(
    clazz: KClass<R>,
    autoMarshalling: AutoMarshalling = CloudWatchMoshi,
) : AwsJsonAction<R>(
    AwsService.of("GraniteServiceVersion20100801"),
    clazz,
    autoMarshalling,
    ContentType.APPLICATION_JSON,
) {

    // Needs extra parameters next to the ones from the super method. See also
    // https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/making-api-requests.html#CloudWatch-API-requests-using-post-method
    override fun toRequest() = super.toRequest()
        .header("Content-Encoding", "amz-1.0")
        .accept(ContentType.APPLICATION_JSON)
}
