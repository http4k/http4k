package org.http4k.format

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.contract.jsonschema.SchemaNodeMarshallingContract

class SchemaNodeMarshallingJacksonTest : SchemaNodeMarshallingContract<JsonNode>(Jackson)
