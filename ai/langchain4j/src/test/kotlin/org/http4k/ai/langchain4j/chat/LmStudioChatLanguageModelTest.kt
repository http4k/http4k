package org.http4k.ai.langchain4j.chat

import org.http4k.ai.langchain4j.LmStudioTestCase
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Temperature
import org.http4k.connect.lmstudio.CHAT_MODEL
import org.http4k.connect.lmstudio.FakeLmStudio
import org.http4k.connect.lmstudio.Http
import org.http4k.connect.lmstudio.LmStudio

class LmStudioChatLanguageModelTest : LmStudioTestCase(), ChatLanguageModelContract {
    override val model by lazy {
        LmStudioChatLanguageModel(
            LmStudio.Http(FakeLmStudio()),
            LmStudioChatModelOptions(ModelName.CHAT_MODEL, temperature = Temperature.ZERO)
        )
    }
}
