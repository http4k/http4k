import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Uri
import org.http4k.lens.int
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.capability.PromptCapability
import org.http4k.mcp.capability.ToolCapability
import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.ModelSelector
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Tool
import org.http4k.routing.bind
import org.http4k.routing.compose
import org.jsoup.Jsoup

fun LinksOnPage(http: HttpHandler): ResourceHandler = {
    val htmlPage = http(org.http4k.core.Request(GET, it.uri))

    val links = Jsoup.parse(htmlPage.bodyString())
        .allElements.toList()
        .filter { it.tagName() == "a" }
        .filter { it.hasAttr("href") }
        .map {
            Resource.Content.Text(
                it.text(),
                Uri.of(it.attr("href"))
            )
        }
    ResourceResponse(links)
}

fun sampling() = compose(
    sampleFromModel()
)

fun tools() = compose(
    liveWeatherTool(),
    reverseTool(),
    countingTool()
)

fun resources() = compose(
    staticResource(),
    templatedResource()
)

fun completions() = compose(
    Reference.Prompt("prompt2") bind {
        CompletionResponse(Completion(listOf("1", "2")))
    }
)

fun prompts() = compose(
    prompt1(),
    prompt2()
)

fun countingTool(): ToolCapability {
    val first = Tool.Arg.int().required("first")
    val second = Tool.Arg.int().required("second")
    return Tool("count", "description", first, second) bind {
        ToolResponse.Ok(listOf(Content.Text(first(it) + second(it))))
    }
}

fun reverseTool(): ToolCapability {
    val input = Tool.Arg.required("name")

    return Tool("reverse", "description", input) bind {
        ToolResponse.Ok(listOf(Content.Text(input(it).reversed())))
    }
}

fun liveWeatherTool(): ToolCapability {
    val input = Tool.Arg.required("city")

    return Tool(
        "weather",
        "checks the weather for a particular city. returns the format '<weather>, <temperature> degrees'",
        input
    ) bind {
        ToolResponse.Ok(listOf(Content.Text("Sunny and 100 degrees")))
    }
}

fun templatedResource() = Resource.Templated(
    Uri.of("https://www.http4k.org/ecosystem/{+ecosystem}/"),
    "HTTP4K ecosystem page",
    "view ecosystem"
) bind LinksOnPage(JavaHttpClient())

fun staticResource() =
    Resource.Static(Uri.of("https://www.http4k.org"), "HTTP4K", "description") bind LinksOnPage(JavaHttpClient())

fun prompt1() = Prompt("prompt1", "description1") bind {
    PromptResponse(listOf(Message(Role.assistant, Content.Text(it.toString()))), "description")
}

fun prompt2(): PromptCapability {
    val arg1 = Prompt.Arg.required("a1", "d1")
    val arg2 = Prompt.Arg.int().optional("a2", "d2")
    return Prompt(
        "prompt2", "description1",
        arg1,
        arg2
    ) bind {
        PromptResponse(listOf(Message(Role.assistant, Content.Text(arg1(it) + arg2(it)))), "description")
    }
}

fun sampleFromModel() = ModelSelector(ModelIdentifier.of("my model")) bind {
    listOf(
        SamplingResponse(
            ModelIdentifier.of("my model"),
            null,
            Role.assistant,
            Content.Text("content1")
        ),
        SamplingResponse(
            ModelIdentifier.of("my model"),
            StopReason.of("end"),
            Role.assistant,
            Content.Text("content2")
        )
    ).asSequence()
}
