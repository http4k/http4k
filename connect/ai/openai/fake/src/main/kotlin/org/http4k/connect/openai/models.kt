package org.http4k.connect.openai

import org.http4k.connect.model.Timestamp
import org.http4k.connect.openai.OpenAIOrg.Companion.OPENAI
import org.http4k.connect.openai.action.Model
import org.http4k.connect.openai.action.Permission
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage

val davinciModel = Model(
    ObjectId.of("text-davinci-002"),
    ObjectType.Model,
    Timestamp.of(1649358449),
    OPENAI,
    listOf(
        Permission(
            ObjectId.of("modelperm-otmQSS0hmabtVGHI9QB3bct3"),
            ObjectType.ModelPermission,
            Timestamp.of(1669085501),
            allow_create_engine = false,
            allow_sampling = true,
            allow_logprobs = true,
            allow_search_indices = false,
            allow_view = true,
            allow_fine_tuning = false,
            organization = OpenAIOrg.ALL,
            group = null,
            is_blocking = false
        )
    ),
    ObjectId.of("text-davinci-002"),
    null
)

val embeddingModel = Model(
    ObjectId.of("text-embedding-ada-002"),
    ObjectType.Model,
    Timestamp.of(1649358449),
    OPENAI,
    listOf(
        Permission(
            ObjectId.of("modelperm-text-embedding-ada-002"),
            ObjectType.ModelPermission,
            Timestamp.of(1669085501),
            allow_create_engine = false,
            allow_sampling = true,
            allow_logprobs = true,
            allow_search_indices = false,
            allow_view = true,
            allow_fine_tuning = false,
            organization = OpenAIOrg.ALL,
            group = null,
            is_blocking = false
        )
    ),
    ObjectId.of("text-davinci-002"),
    null
)

val curieModel = Model(
    ObjectId.of("text-curie-002"),
    ObjectType.Model,
    Timestamp.of(1649358450),
    OPENAI,
    listOf(
        Permission(
            ObjectId.of("modelperm-49FUp5v084tBB49tC4z8LPH5"),
            ObjectType.ModelPermission,
            Timestamp.of(1669085502),
            allow_create_engine = false,
            allow_sampling = true,
            allow_logprobs = true,
            allow_search_indices = false,
            allow_view = true,
            allow_fine_tuning = false,
            organization = OpenAIOrg.ALL,
            group = null,
            is_blocking = false
        )
    ),
    ObjectId.of("text-curie-002"),
    null
)

val gpt4Model = Model(
    ObjectId.of("gpt-4"),
    ObjectType.Model,
    Timestamp.of(1687882411),
    OPENAI,
    listOf(
    ),
    null,
    null
)

val ada002Model = Model(
    ObjectId.of("text-embedding-ada-002"),
    ObjectType.Model,
    Timestamp.of(1687882411),
    OPENAI,
    listOf(
    ),
    null,
    null
)

val DEFAULT_OPEN_AI_MODELS = Storage.InMemory<Model>().apply {
    setOf(ada002Model, gpt4Model, curieModel, davinciModel, embeddingModel).forEach {
        set(it.id.value, it)
    }
}
