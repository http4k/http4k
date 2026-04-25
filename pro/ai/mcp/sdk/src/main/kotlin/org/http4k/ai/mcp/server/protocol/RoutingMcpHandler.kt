/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcEmptyResponse
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpPing
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpRoot
import org.http4k.ai.mcp.protocol.messages.McpSampling
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.ClientCall
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.jsonrpc.ErrorMessage
import kotlin.random.Random

fun RoutingMcpHandler(
    initializer: Initializer,
    clientTracking: MutableMap<Session, ClientTracking>,
    completions: Completions,
    prompts: Prompts,
    resources: Resources,
    tools: Tools,
    logger: Logger,
    tasks: Tasks,
    cancellations: Cancellations,
    roots: Roots,
    random: Random,
    sessions: Sessions<*>,
): McpHandler {

    fun clientFor(session: Session): SessionBasedClient = SessionBasedClient(
        { sessions.send(ClientCall(session), it) },
        session,
        logger,
        tasks,
        roots,
        random,
        { clientTracking[session] ?: throw McpException(ErrorMessage.InternalError) }
    )

    return { mcp ->
        when (mcp.message) {
            is McpInitialize.Request -> {
                val initialize = initializer(mcp.message.params, mcp.http)
                clientTracking[mcp.session] = ClientTracking(mcp.message.params)
                McpResponse.Ok(McpInitialize.Response(initialize, mcp.message.id))
            }

            is McpPing.Request -> McpResponse.Ok(McpJsonRpcEmptyResponse(mcp.message.id))
            is McpCompletion.Request -> McpResponse.Ok(
                McpCompletion.Response(
                    completions.complete(
                        mcp.message.params,
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpPrompt.Get.Request -> McpResponse.Ok(
                McpPrompt.Get.Response(
                    prompts.get(
                        mcp.message.params,
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpPrompt.List.Request -> McpResponse.Ok(
                McpPrompt.List.Response(
                    prompts.list(
                        mcp.message.params ?: McpPrompt.List.Request.Params(),
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpResource.ListTemplates.Request -> McpResponse.Ok(
                McpResource.ListTemplates.Response(
                    resources.listTemplates(
                        mcp.message.params ?: McpResource.ListTemplates.Request.Params(),
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpResource.List.Request -> McpResponse.Ok(
                McpResource.List.Response(
                    resources.listResources(
                        mcp.message.params ?: McpResource.List.Request.Params(),
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpResource.Read.Request -> McpResponse.Ok(
                McpResource.Read.Response(
                    resources.read(
                        mcp.message.params,
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpResource.Subscribe.Request -> {
                if (resources is ObservableResources) resources.subscribe(mcp.session, mcp.message.params) {
                    sessions.send(
                        Subscription(mcp.session),
                        McpResource.Updated.Notification(
                            McpResource.Updated.Notification.Params(mcp.message.params.uri)
                        )
                    )
                }
                McpResponse.Ok(McpJsonRpcEmptyResponse(mcp.message.id))
            }

            is McpResource.Unsubscribe.Request -> {
                if (resources is ObservableResources) resources.unsubscribe(mcp.session, mcp.message.params)
                McpResponse.Ok(McpJsonRpcEmptyResponse(mcp.message.id))
            }

            is McpLogging.SetLevel.Request -> {
                logger.setLevel(mcp.session, mcp.message.params.level)
                McpResponse.Ok(McpJsonRpcEmptyResponse(mcp.message.id))
            }

            is McpTool.Call.Request -> McpResponse.Ok(
                McpTool.Call.Response(
                    tools.call(
                        mcp.message.params,
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpTool.List.Request -> McpResponse.Ok(
                McpTool.List.Response(
                    tools.list(
                        mcp.message.params ?: McpTool.List.Request.Params(),
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpTask.Get.Request -> McpResponse.Ok(
                McpTask.Get.Response(
                    tasks.get(
                        mcp.session,
                        mcp.message.params,
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpTask.Result.Request -> McpResponse.Ok(
                McpTask.Result.Response(
                    tasks.result(
                        mcp.session,
                        mcp.message.params,
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpTask.Cancel.Request -> McpResponse.Ok(
                McpTask.Cancel.Response(
                    tasks.cancel(
                        mcp.session,
                        mcp.message.params,
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpTask.List.Request -> McpResponse.Ok(
                McpTask.List.Response(
                    tasks.list(
                        mcp.session,
                        mcp.message.params,
                        clientFor(mcp.session),
                        mcp.http
                    ), mcp.message.id
                )
            )

            is McpInitialize.Initialized.Notification -> McpResponse.Accepted
            is McpProgress.Notification -> McpResponse.Accepted
            is McpCancelled.Notification -> {
                cancellations.cancel(mcp.message.params)
                McpResponse.Accepted
            }

            is McpTask.Status.Notification -> {
                tasks.update(mcp.session, mcp.message.params)
                McpResponse.Accepted
            }

            is McpRoot.Changed.Notification -> {
                roots.changed(
                    mcp.message.params ?: McpRoot.Changed.Notification.Params(),
                    clientFor(mcp.session),
                    mcp.http
                )
                McpResponse.Accepted
            }

            is McpPrompt.List.Changed.Notification -> McpResponse.Accepted
            is McpTool.List.Changed.Notification -> McpResponse.Accepted
            is McpResource.List.Changed.Notification -> McpResponse.Accepted
            is McpResource.Updated.Notification -> McpResponse.Accepted
            is McpLogging.LoggingMessage.Notification -> McpResponse.Accepted
            is McpElicitations.Complete.Notification -> McpResponse.Accepted
            is McpSampling.Request -> McpResponse.Accepted
            is McpElicitations.Request -> McpResponse.Accepted
            is McpRoot.List.Request -> McpResponse.Accepted
        }
    }
}
