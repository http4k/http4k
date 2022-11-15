package org.http4k.contract.openapi

object AddSimpleFieldToRootNode : OpenApiExtension {
    override fun <NODE : Any> invoke(node: NODE): Render<NODE> = {
        obj(fields(node) + ("x-extension" to array(string("extensionField"))))
    }
}
