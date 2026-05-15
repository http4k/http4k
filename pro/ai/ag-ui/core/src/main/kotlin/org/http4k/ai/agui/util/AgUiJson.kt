/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.ai.agui.model.ActivityType
import org.http4k.ai.agui.model.MessageId
import org.http4k.ai.agui.model.RunId
import org.http4k.ai.agui.model.StepName
import org.http4k.ai.agui.model.ThreadId
import org.http4k.ai.agui.model.ToolCallId
import org.http4k.ai.util.withAiMappings
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.MoshiNodeAdapter
import org.http4k.format.SetAdapter
import org.http4k.format.ThrowableAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object AgUiJson : ConfigurableAgUiJson()

abstract class ConfigurableAgUiJson(
    customJsonFactory: JsonAdapter.Factory = JsonAdapter.Factory { _, _, _ -> null },
    customMappings: AutoMappingConfiguration<Moshi.Builder>.() -> AutoMappingConfiguration<Moshi.Builder> = { this }
) : ConfigurableMoshi(
    Moshi.Builder()
        .add(AgUiJsonFactory)
        .addLast(ThrowableAdapter)
        .addLast(ListAdapter)
        .addLast(SetAdapter)
        .addLast(MapAdapter)
        .addLast(MoshiNodeAdapter)
        .addLast(customJsonFactory)
        .asConfigurable()
        .apply { customMappings() }
        .withAgUiMappings()
        .done()
)

@KotshiJsonAdapterFactory
object AgUiJsonFactory : JsonAdapter.Factory by KotshiAgUiJsonFactory

fun <T> AutoMappingConfiguration<T>.withAgUiMappings() = apply {
    withStandardMappings()
    withAiMappings()
    value(ActivityType)
    value(MessageId)
    value(RunId)
    value(StepName)
    value(ThreadId)
    value(ToolCallId)
}
