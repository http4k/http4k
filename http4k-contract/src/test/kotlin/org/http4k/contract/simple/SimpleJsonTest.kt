package org.http4k.contract.simple

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.contract.ContractRendererContract
import org.http4k.format.Jackson

class SimpleJsonTest : ContractRendererContract<JsonNode>(Jackson, SimpleJson(Jackson))
