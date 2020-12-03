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
    operator fun invoke(req: Response): Resp
}

class RequestContent(
    private val path: String,
    private val queryStringParameters: Map<String, String>?,
    private val rawQueryString: String?,
    private val reqBody: String?,
    private val reqBase64: Boolean?, private val reqMethod: String,
    private val reqHeaders: Map<String, List<String>>?,
    private val cookies: List<String>) {

    fun asHttp4k(): Request {
        val body = reqBody?.let { MemoryBody(if (reqBase64 == true) Base64.getDecoder().decode(it.toByteArray()) else it.toByteArray()) } ?: Body.EMPTY
        return (reqHeaders ?: emptyMap()).map { (k, vs) -> vs.map { v -> k to v } }.toList().flatten().fold(
            Request(Method.valueOf(reqMethod), uri()).body(body)) { memo, (fst, snd) ->
            memo.header(fst, snd)
        }.headers(cookies.map { "Cookie" to it })
    }

    private fun uri(): Uri {
        val query = queryStringParameters?.toList() ?: Uri.of(rawQueryString.orEmpty()).queries()
        return Uri.of(path).query(query.toUrlFormEncoded())
    }

}
