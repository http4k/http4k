package org.http4k.connect.ollama

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Role
import org.http4k.connect.ollama.action.ChatCompletion
import org.http4k.connect.ollama.action.ModelOptions
import org.http4k.connect.successValue
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class ToolCallingTest {
    private val model = ModelName.of("llama3.2")
    private val tool = Tool(
        FunctionSpec(
            name = "get_weather",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf("city" to mapOf("type" to "string", "enum" to listOf("Paris", "London"))),
                "required" to listOf("city"),
                "additionalProperties" to false
            ),
            description = "Get the weather for a city"
        )
    )
    private val toolCall = ToolCall(FunctionCall("get_weather", mapOf("city" to "Paris")))
    private val response = """
        {
          "model":"llama3.2",
          "created_at":"2025-10-26T01:48:48Z",
          "message":{
            "role":"assistant",
            "content":"",
            "tool_calls":[{"function":{"name":"get_weather","arguments":{"city":"Paris"}}}]
          },
          "done":true
        }
    """.trimIndent()

    @Test
    fun `send tool definitions through the generated client extension`() {
        val ollama = Ollama.Http { request ->
            assertThat(request.method, equalTo(POST))
            assertThat(request.uri.path, equalTo("/api/chat"))
            assertThat(
                json(request.bodyString()),
                equalTo(
                    json(
                        """
                        {
                          "model":"llama3.2",
                          "messages":[{"role":"user","content":"What is the weather?"}],
                          "stream":false,
                          "tools":[{
                            "type":"function",
                            "function":{
                              "name":"get_weather",
                              "description":"Get the weather for a city",
                              "parameters":{
                                "type":"object",
                                "properties":{"city":{"type":"string","enum":["Paris","London"]}},
                                "required":["city"],
                                "additionalProperties":false
                              }
                            }
                          }]
                        }
                        """.trimIndent()
                    )
                )
            )
            jsonResponse(response)
        }

        ollama.chatCompletion(model, listOf(Message.User("What is the weather?")), false, tools = listOf(tool))
            .successValue().toList()
    }

    @Test
    fun `single message overload can send tools`() {
        val request = ChatCompletion(model, Message.User("hello"), tools = listOf(tool))
        val ollama = Ollama.Http { actual ->
            assertThat(json(actual.bodyString()), equalTo(json(request.toRequest().bodyString())))
            jsonResponse(response)
        }

        assertThat(request.tools, equalTo(listOf(tool) as List<Tool>?))
        ollama.chatCompletion(model, Message.User("hello"), false, tools = listOf(tool)).successValue().toList()
    }

    @Test
    fun `decode a tool call without optional id or index`() {
        val ollama = Ollama.Http { jsonResponse(response) }
        val message = ollama.chatCompletion(model, listOf(Message.User("weather")), false)
            .successValue().single().message!!

        assertThat(message.role, equalTo(Role.Assistant))
        assertThat(message.content, equalTo(""))
        assertThat(message.tool_calls, equalTo(listOf(toolCall) as List<ToolCall>?))
    }

    @Test
    fun `decode multiple calls with nested arguments and optional metadata`() {
        val message = OllamaMoshi.asA<Message>(
            """
            {
              "role":"assistant",
              "content":"",
              "tool_calls":[
                {"id":"call_1","function":{"index":0,"name":"get_weather","arguments":{"city":"Paris"}}},
                {"id":"call_2","function":{"index":1,"name":"search","arguments":{
                  "filters":{"enabled":true,"limits":[1,2]},"query":"forecast"
                }}}
              ]
            }
            """.trimIndent()
        )

        assertThat(message.tool_calls?.map { it.id }, equalTo(listOf("call_1", "call_2") as List<String?>?))
        assertThat(message.tool_calls?.map { it.function.index }, equalTo(listOf(0, 1) as List<Int?>?))
        assertThat(message.tool_calls?.map { it.function.name }, equalTo(listOf("get_weather", "search") as List<String>?))
        assertThat(
            message.tool_calls!![1].function.arguments,
            equalTo(mapOf<String, Any>("filters" to mapOf("enabled" to true, "limits" to listOf(1.0, 2.0)), "query" to "forecast"))
        )
        assertThat(OllamaMoshi.asA<Message>(OllamaMoshi.asFormatString(message)), equalTo(message))
    }

    @Test
    fun `preserve calls in streaming JSON lines`() {
        val chunk = response.replace("\n", "").replace("\"done\":true", "\"done\":false")
        val finalChunk = """{"model":"llama3.2","created_at":"2025-10-26T01:48:49Z","message":{"role":"assistant","content":""},"done":true}"""
        val ollama = Ollama.Http { Response(OK).header("Content-Type", "application/x-ndjson").body("$chunk\n\n$finalChunk\n") }
        val responses = ollama.chatCompletion(model, listOf(Message.User("weather")), true).successValue().toList()

        assertThat(responses.size, equalTo(2))
        assertThat(responses[0].message?.tool_calls, equalTo(listOf(toolCall) as List<ToolCall>?))
        assertThat(responses.map { it.done }, equalTo(listOf(false, true)))
    }

    @Test
    fun `send assistant calls and tool results in the next request`() {
        var requests = 0
        val ollama = Ollama.Http { request ->
            if (++requests == 2) {
                val messages = json(request.bodyString())["messages"] as List<*>
                assertThat(
                    messages[1],
                    equalTo(json("""{"role":"assistant","content":"","tool_calls":[{"function":{"name":"get_weather","arguments":{"city":"Paris"}}}]}""") as Any?)
                )
                assertThat(
                    messages[2],
                    equalTo(json("""{"role":"tool","content":"Sunny","tool_name":"get_weather"}""") as Any?)
                )
            }
            jsonResponse(response)
        }
        val user = Message.User("weather")
        val assistant = ollama.chatCompletion(model, listOf(user), false, tools = listOf(tool)).successValue().single().message!!

        ollama.chatCompletion(
            model,
            listOf(user, assistant, Message.ToolCallResult("get_weather", "Sunny")),
            false,
            tools = listOf(tool)
        ).successValue().toList()
        assertThat(requests, equalTo(2))
    }

    @Test
    fun `tool helpers preserve call ids and tool names`() {
        val assistant = Message.ToolCalls(listOf(toolCall.copy(id = "call_1")))
        val result = Message.ToolCallResult("get_weather", "Sunny", "call_1")

        assertThat(assistant.role, equalTo(Role.Assistant))
        assertThat(assistant.content, equalTo(""))
        assertThat(
            json(OllamaMoshi.asFormatString(result)),
            equalTo(json("""{"role":"tool","content":"Sunny","tool_name":"get_weather","tool_call_id":"call_1"}"""))
        )
        assertThat(OllamaMoshi.asA<Message>(OllamaMoshi.asFormatString(result)), equalTo(result))
    }

    @Test
    fun `ordinary chat keeps the existing request shape`() {
        val action = ChatCompletion(model, listOf(Message.User("hello")))
        assertThat(
            json(action.toRequest().bodyString()),
            equalTo(json("""{"model":"llama3.2","messages":[{"role":"user","content":"hello"}],"stream":false}"""))
        )
        assertThat(
            OllamaMoshi.asA<Message>("""{"role":"assistant","content":"hello"}"""),
            equalTo(Message.Assistant("hello"))
        )
        val ollama = Ollama.Http { request ->
            assertThat(json(request.bodyString()), equalTo(json(action.toRequest().bodyString())))
            jsonResponse(response)
        }
        ollama.chatCompletion(model, Message.User("hello"), false).successValue().toList()
    }

    @Test
    fun `handle empty calls and empty arguments`() {
        assertThat(
            OllamaMoshi.asA<Message>("""{"role":"assistant","content":"hello","tool_calls":[]}""").tool_calls,
            equalTo(emptyList<ToolCall>() as List<ToolCall>?)
        )
        val call = ToolCall(FunctionCall("get_time", emptyMap()))
        assertThat(OllamaMoshi.asA<ToolCall>("""{"function":{"name":"get_time","arguments":{}}}"""), equalTo(call))
        assertThat(json(OllamaMoshi.asFormatString(call)), equalTo(json("""{"function":{"name":"get_time","arguments":{}}}""")))
    }

    @Test
    fun `null tool metadata is accepted and omitted when serialized`() {
        val message = OllamaMoshi.asA<Message>(
            """{"role":"assistant","content":"hello","tool_calls":null,"tool_name":null,"tool_call_id":null}"""
        )
        assertThat(message, equalTo(Message.Assistant("hello")))
        assertThat(json(OllamaMoshi.asFormatString(message)), equalTo(json("""{"role":"assistant","content":"hello"}""")))
        assertThat(
            json(OllamaMoshi.asFormatString(Tool(FunctionSpec("get_time", mapOf("type" to "object"))))),
            equalTo(json("""{"type":"function","function":{"name":"get_time","parameters":{"type":"object"}}}"""))
        )
    }

    @Test
    fun `legacy copy overloads retain new fields`() {
        val action = ChatCompletion(model, listOf(Message.User("hello")), tools = listOf(tool))
        val message = Message(Role.Tool, "Sunny", tool_calls = listOf(toolCall), tool_name = "get_weather", tool_call_id = "call_1")

        assertThat(action.copy(stream = true).tools, equalTo(listOf(tool) as List<Tool>?))
        assertThat(message.copy(content = "Cloudy"), equalTo(message.copy(content = "Cloudy", tool_call_id = "call_1")))
        assertThat(action.copy(tools = null).tools, equalTo(null))
        assertThat(message.copy(tool_calls = null).tool_calls, equalTo(null))
    }

    @Test
    fun `retain original JVM constructors and copy entry points`() {
        val marker = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")
        val actionParameters = arrayOf(
            ModelName::class.java, List::class.java, Boolean::class.javaObjectType,
            ResponseFormat::class.java, String::class.java, ModelOptions::class.java
        )
        val messageParameters = arrayOf(Role::class.java, String::class.java, List::class.java)
        ChatCompletion::class.java.getConstructor(*actionParameters)
        ChatCompletion::class.java.getConstructor(*actionParameters, Int::class.javaPrimitiveType, marker)
        val singleMessageParameters = actionParameters.copyOf().apply { this[1] = Message::class.java }
        ChatCompletion::class.java.getConstructor(*singleMessageParameters)
        ChatCompletion::class.java.getConstructor(*singleMessageParameters, Int::class.javaPrimitiveType, marker)
        Message::class.java.getConstructor(*messageParameters)
        Message::class.java.getConstructor(*messageParameters, Int::class.javaPrimitiveType, marker)
        ChatCompletion::class.java.getMethod("copy", *actionParameters)
        ChatCompletion::class.java.getMethod("copy\$default", ChatCompletion::class.java, *actionParameters, Int::class.javaPrimitiveType, Any::class.java)
        Message::class.java.getMethod("copy", *messageParameters)
        Message::class.java.getMethod("copy\$default", Message::class.java, *messageParameters, Int::class.javaPrimitiveType, Any::class.java)
        val extensions = Class.forName("org.http4k.connect.ollama.OllamaExtensionsKt")
        extensions.getMethod("chatCompletion", Ollama::class.java, *actionParameters)
        extensions.getMethod("chatCompletion\$default", Ollama::class.java, *actionParameters, Int::class.javaPrimitiveType, Any::class.java)
        extensions.getMethod("chatCompletion", Ollama::class.java, *singleMessageParameters)
        extensions.getMethod("chatCompletion\$default", Ollama::class.java, *singleMessageParameters, Int::class.javaPrimitiveType, Any::class.java)
    }

    private fun json(value: String) = OllamaMoshi.asA<Map<String, Any>>(value)

    private fun jsonResponse(value: String) = Response(OK).header("Content-Type", "application/json").body(value)
}
