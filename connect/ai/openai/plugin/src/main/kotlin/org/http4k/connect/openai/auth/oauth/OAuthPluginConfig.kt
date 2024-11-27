package org.http4k.connect.openai.auth.oauth

import org.http4k.connect.openai.auth.OpenAIPluginId
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri
import org.http4k.security.OAuthProviderConfig

/**
 * Standard OAuth config setup.
 */
data class OAuthPluginConfig(
    val pluginId: OpenAIPluginId,
    val providerConfig: OAuthProviderConfig,
    val redirectionUris: List<Uri> = listOf(Uri.of("https://chat.openai.com/aip/plugin-${pluginId}/oauth/callback")),
    val scope: String = "",
    val contentType: ContentType = APPLICATION_JSON.withNoDirectives(),
)

