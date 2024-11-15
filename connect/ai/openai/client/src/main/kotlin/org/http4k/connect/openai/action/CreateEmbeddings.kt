@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.openai.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.model.ModelName
import org.http4k.connect.openai.ObjectType
import org.http4k.connect.openai.ObjectType.Companion.Embedding
import org.http4k.connect.openai.ObjectType.Companion.List
import org.http4k.connect.openai.OpenAIAction
import org.http4k.connect.openai.OpenAIMoshi
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateEmbeddings(val model: ModelName, val input: List<String>) :
    NonNullAutoMarshalledAction<Embeddings>(kClass(), OpenAIMoshi),
    OpenAIAction<Embeddings> {
    override fun toRequest() = Request(POST, "/v1/embeddings")
        .with(OpenAIMoshi.autoBody<CreateEmbeddings>().toLens() of this)
}

@JsonSerializable
data class Embedding(val embedding: FloatArray, val index: Int) {

    @JsonProperty(name = "object")
    val objectType: ObjectType = Embedding
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Embedding

        if (!embedding.contentEquals(other.embedding)) return false
        if (index != other.index) return false
        if (objectType != other.objectType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = embedding.contentHashCode()
        result = 31 * result + index
        result = 31 * result + objectType.hashCode()
        return result
    }
}

@JsonSerializable
data class Embeddings(val `data`: List<Embedding>, val model: ModelName, val usage: Usage) {
    @JsonProperty(name = "object")
    val objectType: ObjectType = List
}
