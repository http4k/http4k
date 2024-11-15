package org.http4k.connect.ollama

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.Role
import se.ansman.kotshi.JsonSerializable

class Prompt private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Prompt>(::Prompt)
}

class SystemMessage private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SystemMessage>(::SystemMessage)
}

class Template private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Template>(::Template)
}

enum class ResponseFormat {
    json
}

@JsonSerializable
data class Message(val role: Role, val content: String, val images: List<Base64Blob>? = null) {
    companion object {
        fun User(content: String, images: List<Base64Blob>? = null) = Message(Role.User, content, images)
        fun System(content: String, images: List<Base64Blob>? = null) = Message(Role.System, content, images)
        fun Assistant(content: String, images: List<Base64Blob>? = null) = Message(Role.Assistant, content, images)
    }
}
