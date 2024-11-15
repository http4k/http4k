package org.http4k.connect.lmstudio

import org.http4k.connect.lmstudio.Org.Companion.DEFAULT
import org.http4k.connect.lmstudio.action.Model
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage

val chatModel = Model(
    ObjectId.of("chat-model"),
    ObjectType.Model,
    DEFAULT
)

val embeddingModel = Model(
    ObjectId.of("embedding-model"),
    ObjectType.Model,
    DEFAULT,
)

val DEFAULT_MODELS = Storage.InMemory<Model>().apply {
    setOf(chatModel, embeddingModel).forEach {
        set(it.id.value, it)
    }
}
