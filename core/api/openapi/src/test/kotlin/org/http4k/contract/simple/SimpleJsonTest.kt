package org.http4k.contract.simple

import org.http4k.contract.ContractRendererContract
import org.http4k.format.Jackson
import tools.jackson.databind.JsonNode

class SimpleJsonTest : ContractRendererContract<JsonNode>(Jackson, SimpleJson(Jackson))
