package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.format.MoshiObject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface ElicitationModel {
    fun string(vararg metadata: Elicitation.Metadata<String, *>) =
        object : ReadOnlyProperty<ElicitationModel, String> {
            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
                (thisRef.asJsonObject() as MoshiObject)[property.name]!!
                    .let { McpJson.text(it) }
        }

    fun optionalString(vararg metadata: Elicitation.Metadata<String, *>) =
        object : ReadOnlyProperty<ElicitationModel, String?> {
            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
                (thisRef.asJsonObject() as MoshiObject)[property.name]
                    ?.let { McpJson.text(it) }
        }

    fun long(vararg metadata: Elicitation.Metadata<Long, *>) = object : ReadOnlyProperty<ElicitationModel, Long> {
        override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
            (thisRef.asJsonObject() as MoshiObject)[property.name]!!
                .let { McpJson.integer(it) }
    }

    fun optionalLong(vararg metadata: Elicitation.Metadata<Long, *>) =
        object : ReadOnlyProperty<ElicitationModel, Long?> {
            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
                (thisRef.asJsonObject() as MoshiObject)[property.name]
                    ?.let { McpJson.integer(it) }
        }

    fun integer(vararg metadata: Elicitation.Metadata<Int, *>) =
        object : ReadOnlyProperty<ElicitationModel, Int> {
            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
                (thisRef.asJsonObject() as MoshiObject)[property.name]!!
                    .let { McpJson.integer(it) }
                    .toInt()
        }

    fun optionalIntegar(vararg metadata: Elicitation.Metadata<Int, *>) =
        object : ReadOnlyProperty<ElicitationModel, Int?> {
            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
                (thisRef.asJsonObject() as MoshiObject)[property.name]
                    ?.let { McpJson.integer(it) }
                    ?.toInt()
        }

    fun boolean(vararg metadata: Elicitation.Metadata<Boolean, *>) =
        object : ReadOnlyProperty<ElicitationModel, Boolean> {
            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
                (thisRef.asJsonObject() as MoshiObject)[property.name]!!
                    .let { McpJson.bool(it) }
        }

    fun optionalBoolean(vararg metadata: Elicitation.Metadata<Boolean, *>) =
        object : ReadOnlyProperty<ElicitationModel, Boolean?> {
            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
                (thisRef.asJsonObject() as MoshiObject)[property.name]
                    ?.let { McpJson.bool(it) }
        }
}
