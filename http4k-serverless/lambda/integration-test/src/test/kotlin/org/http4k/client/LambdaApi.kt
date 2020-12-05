package org.http4k.client

import org.http4k.aws.Region
import org.http4k.core.Filter

object LambdaApi {
    operator fun invoke(region: Region): Filter = Filter { next ->
        { request -> next(request.uri(request.uri.host("lambda.${region.name}.amazonaws.com").scheme("https"))) }
    }
}
