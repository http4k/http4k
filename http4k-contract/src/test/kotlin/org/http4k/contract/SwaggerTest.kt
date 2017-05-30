package org.http4k.contract

import org.http4k.format.Argo

class SwaggerTest : ContractRendererContract(Swagger(ApiInfo("title", "1.2", "module description"), Argo))