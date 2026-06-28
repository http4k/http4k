package org.http4k.contract.openapi.v3

import org.http4k.contract.ContractRendererContract
import org.http4k.contract.openapi.AddSimpleFieldToRootNode
import org.http4k.contract.openapi.ApiInfo
import org.http4k.core.Uri
import org.http4k.format.Moshi
import org.http4k.format.MoshiNode

private val moshi = Moshi

class OpenApi3MoshiTest : ContractRendererContract<MoshiNode>(
    moshi,
    OpenApi3(
        apiInfo = ApiInfo("title", "1.2", "module description"),
        json = moshi,
        apiRenderer = OpenApi3ApiRenderer(moshi),
        servers = listOf(ApiServer(Uri.of("http://localhost:8000"))),
        extensions = listOf(AddSimpleFieldToRootNode),
    )
)
