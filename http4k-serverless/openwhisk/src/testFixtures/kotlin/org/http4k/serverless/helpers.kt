package org.http4k.serverless

data class FakeOpenWhiskRawRequest(
    val __ow_method: String,
    val __ow_path: String?,
    val __ow_query: String?,
    val __ow_headers: Map<String, String>?,
    val __ow_body: String?
)

data class FakeOpenWhiskRequestWithTopLevelQueries(
    val __ow_method: String,
    val __ow_path: String?,
    val __ow_headers: Map<String, String>?,
    val __ow_body: String?,
    val query: String?
)

data class FakeOpenWhiskResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: String
)
