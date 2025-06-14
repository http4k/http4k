package org.http4k.contract.openapi

import org.http4k.core.Credentials
import org.http4k.security.BasicAuthSecurity

object AddSimpleFieldToRootNode : OpenApiExtension {
    override fun <NODE> invoke(node: NODE): Render<NODE> = {
        obj(fields(node) + ("x-extension" to array(string("extensionField"))))
    }
}

fun security(name: String) = BasicAuthSecurity("", Credentials("", ""), name)

