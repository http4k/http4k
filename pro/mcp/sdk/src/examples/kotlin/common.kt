import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Uri
import org.http4k.lens.int
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SampleResponse
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.ModelScore
import org.http4k.mcp.model.ModelSelector
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Tool
import org.http4k.routing.PromptFeatureBinding
import org.http4k.routing.ToolFeatureBinding
import org.http4k.routing.bind
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

fun llm() = ModelSelector(ModelIdentifier.of("my model")) { ModelScore.MAX } bind {
    SampleResponse(ModelIdentifier.of("my model"), StopReason.of("stop"), Role.assistant, Content.Text("content"))
}

fun countingTool(): ToolFeatureBinding {
    val first = Tool.Arg.int().required("first")
    val second = Tool.Arg.int().required("second")
    return Tool("count", "description", first, second) bind {
        ToolResponse.Ok(listOf(Content.Text(first(it) + second(it))))
    }
}

fun reverseTool(): ToolFeatureBinding {
    val input = Tool.Arg.required("name")

    return Tool("reverse", "description", input) bind {
        ToolResponse.Ok(listOf(Content.Text(input(it).reversed())))
    }
}

fun liveWeatherTool(): ToolFeatureBinding {
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
    PromptResponse("description", listOf(Message(Role.assistant, Content.Text(it.toString()))))
}

fun prompt2(): PromptFeatureBinding {
    val arg1 = Prompt.Arg.required("a1", "d1")
    val arg2 = Prompt.Arg.int().optional("a2", "d2")
    return Prompt(
        "prompt2", "description1",
        arg1,
        arg2
    ) bind {
        PromptResponse("description", listOf(Message(Role.assistant, Content.Text(arg1(it) + arg2(it)))))
    }
}

fun sampleFromModel() = ModelSelector(ModelIdentifier.of("my model")) bind {
    SampleResponse(ModelIdentifier.of("my model"), StopReason.of("end"), Role.assistant, Content.Text("content"))
}
