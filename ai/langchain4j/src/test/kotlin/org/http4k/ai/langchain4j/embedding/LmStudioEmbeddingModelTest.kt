package org.http4k.ai.langchain4j.embedding

import org.http4k.ai.langchain4j.LmStudioTestCase
import org.http4k.ai.model.ModelName
import org.http4k.connect.lmstudio.CHAT_MODEL
import org.http4k.connect.lmstudio.FakeLmStudio
import org.http4k.connect.lmstudio.Http
import org.http4k.connect.lmstudio.LmStudio

class LmStudioEmbeddingModelTest : LmStudioTestCase(), EmbeddingModelContract {
    override val model = LmStudioEmbeddingModel(LmStudio.Http(FakeLmStudio()), ModelName.CHAT_MODEL)
}
