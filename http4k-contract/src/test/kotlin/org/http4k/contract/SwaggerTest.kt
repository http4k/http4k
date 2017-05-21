package org.http4k.contract

import org.http4k.format.Argo

class SwaggerTest : ModuleRendererContract(Swagger(ApiInfo("title", "1.2", "module description"), Argo))