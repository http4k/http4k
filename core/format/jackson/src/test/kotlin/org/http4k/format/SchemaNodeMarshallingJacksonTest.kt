package org.http4k.format

import tools.jackson.databind.JsonNode
import org.http4k.contract.jsonschema.SchemaNodeMarshallingContract

class SchemaNodeMarshallingJacksonTest : SchemaNodeMarshallingContract<JsonNode>(Jackson)
