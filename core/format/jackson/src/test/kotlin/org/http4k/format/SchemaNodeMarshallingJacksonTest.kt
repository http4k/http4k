package org.http4k.format

import org.http4k.contract.jsonschema.SchemaNodeMarshallingContract
import tools.jackson.databind.JsonNode

class SchemaNodeMarshallingJacksonTest : SchemaNodeMarshallingContract<JsonNode>(Jackson)
