@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.mattermost.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex
import org.http4k.core.Uri
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class PostAction(
    val id: PostActionId,
    val type: PostActionType? = null,
    val name: PostActionName,
    val disabled: Boolean? = null,
    val style: PostActionStyle? = null,
    @JsonProperty("data_source")
    val dataSource: PostActionDataSource? = null,
    val options: List<PostActionOption>? = null,
    @JsonProperty("default_option")
    val defaultOption: PostActionOptionValue? = null,
    val integration: PostActionIntegration? = null,
    val cookie: PostActionCookie? = null,
)

class PostActionId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PostActionId>(::PostActionId)
}

enum class PostActionType {
    select, button,
}

class PostActionName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PostActionName>(::PostActionName)
}

class PostActionStyle private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<PostActionStyle>(
        ::PostActionStyle,
        "(default|primary|success|good|warning|danger|#[0-9a-fA-F]{6})".regex,
    ) {
        val default = PostActionStyle("default")
        val primary = PostActionStyle("primary")
        val success = PostActionStyle("success")
        val good = PostActionStyle("good")
        val warning = PostActionStyle("warning")
        val danger = PostActionStyle("danger")
    }
}

enum class PostActionDataSource {
    users, channels,
}

@JsonSerializable
data class PostActionOption(
    val text: PostActionOptionText,
    val value: PostActionOptionValue,
)

class PostActionOptionText private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PostActionOptionText>(::PostActionOptionText)
}

class PostActionOptionValue private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PostActionOptionValue>(::PostActionOptionValue)
}

@JsonSerializable
data class PostActionIntegration(
    val url: Uri,
    val context: Map<String, Any>? = null,
)

class PostActionCookie private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PostActionCookie>(::PostActionCookie)
}
