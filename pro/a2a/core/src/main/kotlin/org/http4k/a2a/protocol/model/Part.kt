package org.http4k.a2a.protocol.model

import org.http4k.connect.model.Base64Blob
import org.http4k.format.MoshiNode
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@Polymorphic("type")
sealed class Part {
    @PolymorphicLabel("text")
    data class Text(val text: String, val metadata: Metadata? = null) : Part()

    @PolymorphicLabel("file")
    data class File(val file: FileContent, val metadata: Metadata? = null) : Part()

    @PolymorphicLabel("data")
    data class Data(val data: MoshiNode, val metadata: Metadata? = null) : Part()
}

