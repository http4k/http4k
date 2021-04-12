package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.Request
import org.http4k.core.Response

interface AwsHttpAdapter<Req, Resp> {
    operator fun invoke(req: Req, ctx: Context): Request
    operator fun invoke(resp: Response): Resp
}
