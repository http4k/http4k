package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.contract.openapi.ApiInfo

class OpenApi3AutoTest : OpenApi3Contract<JsonNode>(OpenApi3(ApiInfo("title", "1.2", "module description")))