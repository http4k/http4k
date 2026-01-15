package blaise

import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.LLMResult
import org.http4k.ai.llm.tools.LLMTools
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.llm.tools.ToolResponse

class DebuggingLLMTools(private val delegate: LLMTools) : LLMTools by delegate {
    override fun invoke(request: ToolRequest): LLMResult<ToolResponse> {
        println(
            "Invoking tool: ${request.name}(${
                request.arguments
                    .map { "\n\t\t" + "${it.key}: ${it.value.toString().take(50)}" }
                    .joinToString()
            })"
        )
        return delegate(request)
            .mapFailure {
                println("Error: $it")
                it
            }
    }
}
