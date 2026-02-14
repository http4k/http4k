package org.http4k.ai.a2a.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.ai.a2a.model.ArtifactId
import org.http4k.ai.a2a.model.AuthScheme
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.SkillId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.protocol.A2ARpcMethod
import org.http4k.ai.util.withAiMappings
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNodeAdapter
import org.http4k.format.SetAdapter
import org.http4k.format.ThrowableAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

typealias A2ANodeType = MoshiNode

object A2AJson : ConfigurableA2AJson()

abstract class ConfigurableA2AJson(
    customJsonFactory: JsonAdapter.Factory = JsonAdapter.Factory { _, _, _ -> null },
    customMappings: AutoMappingConfiguration<Moshi.Builder>.() -> AutoMappingConfiguration<Moshi.Builder> = { this }
) : ConfigurableMoshi(
    Moshi.Builder()
        .add(A2AJsonFactory)
        .addLast(ThrowableAdapter)
        .addLast(ListAdapter)
        .addLast(SetAdapter)
        .addLast(MapAdapter)
        .addLast(MoshiNodeAdapter)
        .addLast(customJsonFactory)
        .asConfigurable()
        .apply { customMappings() }
        .withA2AMappings()
        .done()
)

@KotshiJsonAdapterFactory
object A2AJsonFactory : JsonAdapter.Factory by KotshiA2AJsonFactory

fun <T> AutoMappingConfiguration<T>.withA2AMappings() = apply {
    withStandardMappings()
    withAiMappings()
    value(A2ARpcMethod)
    value(ArtifactId)
    value(AuthScheme)
    value(Base64Blob)
    value(ContextId)
    value(MessageId)
    value(MimeType)
    value(PushNotificationConfigId)
    value(SkillId)
    value(TaskId)
}
