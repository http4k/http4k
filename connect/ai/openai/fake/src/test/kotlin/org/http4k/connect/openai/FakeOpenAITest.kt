package org.http4k.connect.openai

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.openai.action.ImageResponseFormat.b64_json
import org.http4k.connect.openai.action.ImageResponseFormat.url
import org.http4k.connect.openai.action.Size
import org.http4k.connect.successValue
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test

class FakeOpenAITest : OpenAIContract {
    private val fakeOpenAI = FakeOpenAI()
    override val openAi = OpenAI.Http(OpenAIToken.of("hello"), fakeOpenAI)

    @Test
    fun `can generate and serve image from url`(approver: Approver) {
        val generated = openAi.generateImage(
            "An excellent library", Size.`1024x1024`,
            url, 1, Quality.standard, Style.vivid, null
        ).successValue()

        val uri = generated.data.first().url!!
        assertThat(uri, equalTo(Uri.of("http://localhost:45674/1024x1024.png")))
        approver.assertApproved(fakeOpenAI(Request(GET, uri)))
    }

    @Test
    fun `can generate and serve image as data url`(approver: Approver) {
        val generated = openAi.generateImage(
            "An excellent library", Size.`1024x1024`,
            b64_json, 1, Quality.standard, Style.vivid, null
        ).successValue()

        approver.assertApproved(generated.data.first().b64_json!!.value)
    }
}
