package org.http4k.contract

import tools.jackson.databind.JsonNode
import org.http4k.format.Jackson

class JacksonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode>(Jackson)
