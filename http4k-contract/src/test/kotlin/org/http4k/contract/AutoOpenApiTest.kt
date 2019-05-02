package org.http4k.contract

import org.http4k.format.Jackson

class AutoOpenApiTest : ContractRendererContract(AutoOpenApi(ApiInfo("title", "1.2", "module description"), Jackson))