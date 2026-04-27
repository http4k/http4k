/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.protocol.A2ARpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object A2APushNotificationConfig {
    object Set {
        @JsonSerializable
        @PolymorphicLabel("tasks/pushNotificationConfig/set")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("tasks/pushNotificationConfig/set")

            @JsonSerializable
            data class Params(
                val taskId: TaskId,
                val pushNotificationConfig: PushNotificationConfig
            )
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                val id: PushNotificationConfigId,
                val taskId: TaskId,
                val pushNotificationConfig: PushNotificationConfig
            )
        }
    }

    object Get {
        @JsonSerializable
        @PolymorphicLabel("tasks/pushNotificationConfig/get")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("tasks/pushNotificationConfig/get")

            @JsonSerializable
            data class Params(val id: PushNotificationConfigId)
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                val id: PushNotificationConfigId,
                val taskId: TaskId,
                val pushNotificationConfig: PushNotificationConfig
            )
        }
    }

    object List {
        @JsonSerializable
        @PolymorphicLabel("tasks/pushNotificationConfig/list")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("tasks/pushNotificationConfig/list")

            @JsonSerializable
            data class Params(val taskId: TaskId)
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse() {
            @JsonSerializable
            data class Result(val configs: kotlin.collections.List<TaskPushNotificationConfig>)
        }
    }

    object Delete {
        @JsonSerializable
        @PolymorphicLabel("tasks/pushNotificationConfig/delete")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("tasks/pushNotificationConfig/delete")

            @JsonSerializable
            data class Params(val id: PushNotificationConfigId)
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse() {
            @JsonSerializable
            data class Result(val id: PushNotificationConfigId)
        }
    }
}
