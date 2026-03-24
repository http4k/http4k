package org.http4k.connect.ollama

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.model.ModelName
import org.http4k.connect.ollama.action.ChatCompletion
import org.junit.jupiter.api.Test

class ResponseFormatTest {

    @Test
    fun `serialize json format as bare string`() {
        val result = OllamaMoshi.asFormatString(ResponseFormat.json)
        assertThat(result, equalTo("\"json\""))
    }

    @Test
    fun `serialize schema format as JSON object`() {
        val schema = ResponseFormat.Schema(
            mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "name" to mapOf("type" to "string")
                ),
                "required" to listOf("name")
            )
        )
        val result = OllamaMoshi.asFormatString(schema)
        assertThat(
            OllamaMoshi.asA<Map<String, Any>>(result),
            equalTo(
                mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "name" to mapOf("type" to "string")
                    ),
                    "required" to listOf("name")
                )
            )
        )
    }

    @Test
    fun `deserialize bare json string`() {
        val result = OllamaMoshi.asA<ResponseFormat>("\"json\"")
        assertThat(result, equalTo(ResponseFormat.json as ResponseFormat))
    }

    @Test
    fun `deserialize schema object`() {
        val json = """{"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}"""
        val result = OllamaMoshi.asA<ResponseFormat>(json)
        assertThat(
            (result as ResponseFormat.Schema).schema["type"],
            equalTo("object" as Any)
        )
    }

    @Test
    fun `ChatCompletion with json format serializes correctly`() {
        val req = ChatCompletion(
            model = ModelName.of("llama2"),
            messages = listOf(Message.User("hello")),
            format = ResponseFormat.json
        )
        val json = OllamaMoshi.asFormatString(req)
        assertThat(json.contains("\"format\":\"json\""), equalTo(true))
    }

    @Test
    fun `ChatCompletion with schema format serializes correctly`() {
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf("name" to mapOf("type" to "string"))
        )
        val req = ChatCompletion(
            model = ModelName.of("llama2"),
            messages = listOf(Message.User("hello")),
            format = ResponseFormat.Schema(schema)
        )
        val json = OllamaMoshi.asFormatString(req)
        assertThat(json.contains("\"format\":{"), equalTo(true))
    }

    @Test
    fun `ChatCompletion with null format omits field`() {
        val req = ChatCompletion(
            model = ModelName.of("llama2"),
            messages = listOf(Message.User("hello")),
            format = null
        )
        val json = OllamaMoshi.asFormatString(req)
        assertThat(json.contains("\"format\""), equalTo(false))
    }
}
