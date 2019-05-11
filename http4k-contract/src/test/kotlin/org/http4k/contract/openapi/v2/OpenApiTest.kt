package org.http4k.contract.openapi.v2

import org.http4k.contract.ContractRendererContract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.format.Jackson

class OpenApiTest : ContractRendererContract(OpenApi(ApiInfo("title", "1.2", "module description"), Jackson))