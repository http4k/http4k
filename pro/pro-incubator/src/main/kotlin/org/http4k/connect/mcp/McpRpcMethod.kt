package org.http4k.connect.mcp

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class McpRpcMethod private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<McpRpcMethod>(::McpRpcMethod) {
        val Initialize = of("initialize")

        val Ping = of("ping")

        val ResourcesList = of("resources/list")
        val ResourcesTemplatesList = of("resources/templates/list")
        val ResourcesRead = of("resources/read")
        val ResourcesSubscribe = of("resources/subscribe")
        val ResourcesUnsubscribe = of("resources/unsubscribe")

        val PromptsList = of("prompts/list")
        val PromptsGet = of("prompts/get")

        val NotificationsCancelled = of("notifications/cancelled")
        val NotificationsInitialized = of("notifications/initialized")
        val NotificationsProgress = of("notifications/progress")
        val NotificationsMessage = of("notifications/message")
        val NotificationsResourcesUpdated = of("notifications/resources/updated")
        val NotificationsResourcesListChanged = of("notifications/resources/list_changed")
        val NotificationsToolsListChanged = of("notifications/tools/list_changed")
        val NotificationsRootsListChanged = of("notifications/roots/list_changed")
        val NotificationsPromptsListChanged = of("notifications/prompts/list_changed")

        val ToolsList = of("tools/list")
        val ToolsCall = of("tools/call")

        val LoggingSetLevel = of("logging/setLevel")
        val SamplingCreateMessage = of("sampling/createMessage")
        val CompletionComplete = of("completion/complete")
        val RootsList = of("roots/list")
    }
}
