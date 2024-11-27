package org.http4k.connect.langchain.chat

import org.http4k.connect.langchain.LmStudioTestCase
import org.http4k.connect.lmstudio.CHAT_MODEL
import org.http4k.connect.lmstudio.FakeLmStudio
import org.http4k.connect.lmstudio.Http
import org.http4k.connect.lmstudio.LmStudio
import org.http4k.connect.model.ModelName

class LmStudioChatLanguageModelTest : LmStudioTestCase(), ChatLanguageModelContract {
    override val model by lazy {
        LmStudioChatLanguageModel(
            LmStudio.Http(FakeLmStudio()),
            LmStudioChatModelOptions(ModelName.CHAT_MODEL, temperature = 0.0)
        )
    }
}
