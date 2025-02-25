package org.http4k.connect.amazon.ses.model

import com.squareup.moshi.Json
import org.http4k.connect.model.Base64Blob
import se.ansman.kotshi.JsonSerializable
import java.nio.charset.Charset

@JsonSerializable
data class EmailContent(
    @Json(name = "Raw") val raw: RawMessage? = null,
    @Json(name = "Simple") val simple: Message? = null,
    @Json(name = "Template") val template: Template? = null
)

@JsonSerializable
data class Template(
    @Json(name = "TemplateData") val templateData: String? = null,
    @Json(name = "TemplateName") val templateName: String? = null
)

@JsonSerializable
data class Content(
    @Json(name = "Data") val data: String,
    @Json(name = "Charset") val charset: Charset? = null
)

@JsonSerializable
data class Body(
    @Json(name = "Html") val html: Content? = null,
    @Json(name = "Text") val text: Content? = null
)

@JsonSerializable
data class RawMessage(
    @Json(name = "Data") val data: Base64Blob
)

@JsonSerializable
data class Message(
    @Json(name = "Body") val body: Body,
    @Json(name = "Subject") val subject: Content,
    @Json(name = "Headers") val headers: List<MessageHeader>? = null
)

@JsonSerializable
data class MessageHeader(
    @Json(name = "Name") val name: String,
    @Json(name = "Value") val value: String
)
