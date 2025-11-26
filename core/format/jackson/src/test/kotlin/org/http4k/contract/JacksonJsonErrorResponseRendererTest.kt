package org.http4k.contract

import org.http4k.format.Jackson
import tools.jackson.databind.JsonNode

class JacksonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode>(Jackson)
