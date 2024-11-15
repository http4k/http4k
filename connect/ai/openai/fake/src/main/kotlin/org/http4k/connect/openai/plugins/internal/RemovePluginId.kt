package org.http4k.connect.openai.plugins.internal

import org.http4k.connect.openai.auth.OpenAIPluginId
import org.http4k.core.Request

internal fun Request.removePluginId(pluginId: OpenAIPluginId) = uri(uri.path(uri.path.removePrefix("/${pluginId}")))
