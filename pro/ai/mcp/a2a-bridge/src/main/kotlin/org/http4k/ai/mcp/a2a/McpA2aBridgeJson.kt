/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.a2a

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.ai.a2a.model.ArtifactId
import org.http4k.ai.a2a.model.AuthScheme
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.OAuthFlowsJsonAdapterFactory
import org.http4k.ai.a2a.model.PageToken
import org.http4k.ai.a2a.model.PartJsonAdapterFactory
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.SecuritySchemeJsonAdapterFactory
import org.http4k.ai.a2a.model.SkillId
import org.http4k.ai.a2a.model.StreamItemJsonAdapterFactory
import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.a2a.protocol.A2ARpcMethod
import org.http4k.ai.a2a.util.A2AJsonFactory
import org.http4k.ai.mcp.util.ConfigurableMcpJson
import org.http4k.format.value
import java.lang.reflect.Type
import org.http4k.ai.a2a.model.TaskId as A2ATaskId

object McpA2aBridgeJson : ConfigurableMcpJson(
    customJsonFactory = CompositeAdapterFactory(
        PartJsonAdapterFactory,
        StreamItemJsonAdapterFactory,
        SecuritySchemeJsonAdapterFactory,
        OAuthFlowsJsonAdapterFactory,
        A2AJsonFactory
    ),
    customMappings = {
        value(A2ARpcMethod)
        value(ArtifactId)
        value(AuthScheme)
        value(ContextId)
        value(MessageId)
        value(PageToken)
        value(PushNotificationConfigId)
        value(SkillId)
        value(A2ATaskId)
        value(Tenant)
        this
    }
)

private class CompositeAdapterFactory(private vararg val factories: JsonAdapter.Factory) : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? =
        factories.firstNotNullOfOrNull { it.create(type, annotations, moshi) }
}
