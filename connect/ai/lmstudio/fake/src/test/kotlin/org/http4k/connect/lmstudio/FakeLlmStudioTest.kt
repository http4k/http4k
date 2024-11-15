package org.http4k.connect.lmstudio

class FakeLlmStudioTest : LmStudioContract {
    private val fakeLmStudio = FakeLmStudio()
    override val lmStudio = LmStudio.Http(fakeLmStudio)
}
