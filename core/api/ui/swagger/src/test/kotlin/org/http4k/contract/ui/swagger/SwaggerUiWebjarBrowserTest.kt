package org.http4k.contract.ui.swagger

import org.http4k.contract.ui.SwaggerUiBrowserContract
import org.junit.jupiter.api.Disabled

@Disabled("flakey")
class SwaggerUiWebjarBrowserTest : SwaggerUiBrowserContract(::swaggerUiWebjar)
