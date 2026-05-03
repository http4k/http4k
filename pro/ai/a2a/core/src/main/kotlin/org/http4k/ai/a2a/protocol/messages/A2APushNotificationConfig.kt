/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.AuthenticationInfo
import org.http4k.ai.a2a.model.PageToken
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.a2a.protocol.A2ARpcMethod.Companion.of
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object A2APushNotificationConfig {
    object Set {
        @JsonSerializable
        @PolymorphicLabel("CreateTaskPushNotificationConfig")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("CreateTaskPushNotificationConfig")

            @JsonSerializable
            data class Params(
                val taskId: TaskId,
                val url: Uri,
                val token: String? = null,
                val authentication: AuthenticationInfo? = null,
                val tenant: Tenant? = null
            )
        }

        @JsonSerializable
        data class Response(val result: TaskPushNotificationConfig, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse
    }

    object Get {
        @JsonSerializable
        @PolymorphicLabel("GetTaskPushNotificationConfig")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("GetTaskPushNotificationConfig")

            @JsonSerializable
            data class Params(val taskId: TaskId, val id: PushNotificationConfigId, val tenant: Tenant? = null)
        }

        @JsonSerializable
        data class Response(val result: TaskPushNotificationConfig, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse
    }

    object List {
        @JsonSerializable
        @PolymorphicLabel("ListTaskPushNotificationConfigs")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("ListTaskPushNotificationConfigs")

            @JsonSerializable
            data class Params(val taskId: TaskId, val pageSize: Int? = null, val pageToken: PageToken? = null, val tenant: Tenant? = null)
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse {
            @JsonSerializable
            data class Result(val configs: kotlin.collections.List<TaskPushNotificationConfig>, val nextPageToken: PageToken? = null)
        }
    }

    object Delete {
        @JsonSerializable
        @PolymorphicLabel("DeleteTaskPushNotificationConfig")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("DeleteTaskPushNotificationConfig")

            @JsonSerializable
            data class Params(val taskId: TaskId, val id: PushNotificationConfigId, val tenant: Tenant? = null)
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse {
            @JsonSerializable
            data class Result(val id: PushNotificationConfigId)
        }
    }
}
