package org.http4k.openapi.v2

import org.http4k.openapi.v3.ComponentsV3Spec
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.PathV3Spec

fun PathV2Spec.asV3(): PathV3Spec {

//    PathV3Spec(operationId)
    return TODO()
}

fun OpenApi2Spec.asV3() = OpenApi3Spec(
    info, paths.mapValues { it.value.mapValues { it.value.asV3() } }, ComponentsV3Spec(components)
)
