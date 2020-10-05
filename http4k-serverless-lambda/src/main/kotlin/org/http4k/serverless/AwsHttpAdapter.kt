package org.http4k.serverless

import org.http4k.core.Body
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import java.util.Base64

internal interface AwsHttpAdapter<Req, Resp> {
    operator fun invoke(req: Req): Request
    operator fun invoke(req: Response): Resp
}

class RequestContent(
    private val uri: Uri,
    private val reqBody: String?,
    private val reqBase64: Boolean?,
    private val reqMethod: String, private val reqHeaders: Map<String, String>?) {
    fun asHttp4k(): Request {
        val body = reqBody?.let { MemoryBody(if (reqBase64 == true) Base64.getDecoder().decode(it.toByteArray()) else it.toByteArray()) } ?: Body.EMPTY
        return (reqHeaders ?: emptyMap()).toList().fold(
            Request(Method.valueOf(reqMethod), uri).body(body)) { memo, (fst, snd) ->
            memo.header(fst, snd)
        }
    }
}
