package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.contract.openapi.ApiInfo
import org.http4k.format.Jackson

class OpenApi3AutoTest : OpenApi3Contract<JsonNode>(Jackson, OpenApi3(ApiInfo("title", "1.2", "module description")))