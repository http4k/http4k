package org.http4k.serverless

data class FakeOpenWhiskRequest(
    val __ow_method: String,
    val __ow_path: String?,
    val __ow_query: Map<String, String>?,
    val __ow_headers: Map<String, String>?,
    val __ow_body: String?
)

data class FakeOpenWhiskResponse(
    val code: Int,
    val headers: Map<String, String>,
    val body: String
)

