package org.http4k.serverless

import org.http4k.core.Request
import org.http4k.core.Response

interface AwsHttpAdapter<Req, Resp> {
    operator fun invoke(req: Req): Request
    operator fun invoke(req: Response): Resp
}
