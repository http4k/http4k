package org.http4k.connect.openai.auth.oauth

import org.http4k.core.Credentials
import org.http4k.core.Uri
import org.http4k.security.OAuthProvider
import org.http4k.security.OAuthProviderConfig

/**
 * Provides defaulted OAuth configuration for an OpenAI plugin.
 */
fun OAuthProvider.Companion.openaiPlugin(pluginBaseUrl: Uri, openAiClientCredentials: Credentials) =
    OAuthProviderConfig(pluginBaseUrl, "/authorize", "/oauth2/token", openAiClientCredentials)
