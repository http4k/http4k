package org.http4k.contract

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.format.Jackson

class JacksonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode>(Jackson)