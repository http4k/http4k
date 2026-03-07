package org.http4k.wiretap.domain

data class Har(val log: HarLog)

data class HarLog(val version: String = "1.2", val creator: HarCreator = HarCreator(), val entries: List<HarEntry>)

data class HarCreator(val name: String = "http4k-wiretap", val version: String = "1.0")

data class HarEntry(
    val startedDateTime: String,
    val time: Long,
    val request: HarRequest,
    val response: HarResponse,
    val timings: HarTimings
)

data class HarRequest(
    val method: String,
    val url: String,
    val httpVersion: String,
    val headers: List<HarHeader>,
    val queryString: List<HarQueryParam>,
    val headersSize: Int,
    val bodySize: Int,
    val postData: HarPostData?
)

data class HarResponse(
    val status: Int,
    val statusText: String,
    val httpVersion: String,
    val headers: List<HarHeader>,
    val content: HarContent,
    val headersSize: Int,
    val bodySize: Int
)

data class HarHeader(val name: String, val value: String)

data class HarQueryParam(val name: String, val value: String)

data class HarPostData(val mimeType: String, val text: String)

data class HarContent(val size: Int, val mimeType: String, val text: String)

data class HarTimings(val send: Int = 0, val wait: Long, val receive: Int = 0)
