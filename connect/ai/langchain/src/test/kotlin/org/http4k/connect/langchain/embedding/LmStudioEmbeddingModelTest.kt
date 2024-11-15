package org.http4k.connect.langchain.embedding

import org.http4k.connect.langchain.LmStudioTestCase
import org.http4k.connect.lmstudio.CHAT_MODEL
import org.http4k.connect.lmstudio.FakeLmStudio
import org.http4k.connect.lmstudio.Http
import org.http4k.connect.lmstudio.LmStudio
import org.http4k.connect.model.ModelName

class LmStudioEmbeddingModelTest : LmStudioTestCase(), EmbeddingModelContract {
    override val model = LmStudioEmbeddingModel(LmStudio.Http(FakeLmStudio()), ModelName.CHAT_MODEL)
}
