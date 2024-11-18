package org.http4k.contract.openapi

import org.http4k.format.Json

typealias Render<NODE> = Json<NODE>.() -> NODE
