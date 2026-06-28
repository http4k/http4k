package org.http4k.contract.openapi.v3

import org.http4k.contract.AutoContractRendererContract
import org.http4k.contract.openapi.AddSimpleFieldToRootNode
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiLicense
import org.http4k.core.Uri
import org.http4k.format.Moshi
import org.http4k.format.MoshiNode
import org.junit.jupiter.api.Disabled

@Disabled
class OpenApi3AutoMoshiTest : AutoContractRendererContract<MoshiNode>(
    Moshi,
    OpenApi3(
        ApiInfo("title", "1.2", "module description", "a short summary", ApiLicense.Apache2_0),
        Moshi,
        listOf(AddSimpleFieldToRootNode),
        listOf(ApiServer(Uri.of("http://localhost:8000"), "a very simple API")),
    )
)
