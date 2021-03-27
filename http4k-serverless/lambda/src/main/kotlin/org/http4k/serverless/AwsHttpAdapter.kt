package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.Body
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.queries
import org.http4k.core.toUrlFormEncoded
import java.util.Base64

interface AwsHttpAdapter<Req, Resp> {
    operator fun invoke(req: Req, ctx: Context): Request
    operator fun invoke(resp: Response): Resp
}
