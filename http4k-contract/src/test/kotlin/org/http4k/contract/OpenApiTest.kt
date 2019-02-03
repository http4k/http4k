package org.http4k.contract

import org.http4k.format.Jackson

class OpenApiTest : ContractRendererContract(OpenApi(ApiInfo("title", "1.2", "module description"), Jackson))