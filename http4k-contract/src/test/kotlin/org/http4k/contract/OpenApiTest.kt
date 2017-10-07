package org.http4k.contract

import org.http4k.format.Argo

class OpenApiTest : ContractRendererContract(OpenApi(ApiInfo("title", "1.2", "module description"), Argo))