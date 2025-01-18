package org.http4k.mcp.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import org.http4k.core.Uri

@JsonTypeInfo(
    use = NAME,
    include = PROPERTY,
    property = "type",
    defaultImpl = Reference.Unknown::class
)
@JsonSubTypes(
    Type(value = Reference.Resource::class, name = "ref/resource"),
    Type(value = Reference.Prompt::class, name = "ref/prompt"),
)
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
