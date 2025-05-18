package org.http4k.security.signature

internal fun formatComponentName(component: SignatureComponent<*>): String {
    val paramString = if (component.params.isEmpty()) "" else {
        component.params.entries.joinToString(";") { (key, value) ->
            "$key=\"$value\""
        }.let { ";$it" }
    }

    return "${component.name}$paramString"
}
