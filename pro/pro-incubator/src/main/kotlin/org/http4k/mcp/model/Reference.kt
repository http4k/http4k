package org.http4k.mcp.model

import org.http4k.core.Uri

sealed interface Reference {
    val type: String

    data class Resource(val uri: Uri) : Reference {
        override val type = "ref/resource"
    }

    data class Prompt(val name: String) : Reference {
        override val type = "ref/prompt"
    }

    data class Unknown(override val type: String) : Reference
}
