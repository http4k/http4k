package org.http4k.gcp

import com.google.api.client.http.LowLevelHttpResponse
import org.http4k.core.Response
import org.http4k.lens.Header

internal class Http4kLowLevelHttpResponse(private val response: Response) : LowLevelHttpResponse() {
    override fun getContent() = response.body.stream

    override fun getContentEncoding() = response.header("Content-Encoding")

    override fun getContentLength() = response.body.length ?: 0

    override fun getContentType() = Header.CONTENT_TYPE(response)?.value

    override fun getStatusLine() = response.run { "$version $status" }

    override fun getStatusCode() = response.status.code

    override fun getReasonPhrase() = response.status.description

    override fun getHeaderCount() = response.headers.size

    override fun getHeaderName(index: Int) = response.headers[index].first

    override fun getHeaderValue(index: Int) = response.headers[index].second
}
