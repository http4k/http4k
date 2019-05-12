package org.http4k.contract.openapi.v3

import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import org.http4k.util.JacksonJsonSchemaCreator

fun OpenApi3(apiInfo: ApiInfo, jackson: ConfigurableJackson = Jackson) =
    OpenApi3(apiInfo, jackson, ApiRenderer.Auto(jackson, JacksonJsonSchemaCreator(jackson)))