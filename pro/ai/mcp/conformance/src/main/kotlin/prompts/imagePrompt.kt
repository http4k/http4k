package prompts

import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.model.Role
import org.http4k.connect.model.MimeType
import org.http4k.routing.bind
import tools.imageContent

fun imagePrompt() = Prompt("test_prompt_with_image", "test_prompt_with_image", title = "Prompt With Image") bind {
    PromptResponse(
        listOf(
            Message(Role.User, Content.Image(imageContent.data, MimeType.IMAGE_PNG)),
            Message(Role.User, Content.Text("Please analyze the image above."))
        )
    )
}
