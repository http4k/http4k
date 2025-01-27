package org.http4k.contract.openapi

/**
 * Provides a way to apply extensions to the OpenAPI JSON document.
 */
interface OpenApiExtension {
    operator fun <NODE> invoke(node: NODE): Render<NODE>
}
