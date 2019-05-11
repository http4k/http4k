package org.http4k.contract.openapi.v3

import org.http4k.contract.ContractRendererContract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.format.Jackson

class OpenApi3Test : ContractRendererContract(OpenApi3(ApiInfo("title", "1.2", "module description"), Jackson))