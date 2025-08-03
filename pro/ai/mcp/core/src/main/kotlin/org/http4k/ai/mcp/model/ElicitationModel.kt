package org.http4k.ai.mcp.model

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import dev.forkhandles.result4k.resultFrom
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

open class ElicitationModel {

    private val data = mutableMapOf<String, Any?>()

    private fun properties(): List<KProperty1<ElicitationModel, *>> {
        val klass = this::class as KClass<ElicitationModel>
        return klass.memberProperties.filter { property: KProperty1<ElicitationModel, *> ->
            property.isAccessible = true
            property.getDelegate(this) is ElicitationModelStringReadWriteProperty<*>
        }
    }

    internal fun validate() = resultFrom { properties().forEach { it.get(this) } }.map { true }.recover { false }

    fun string(title: String, description: String, vararg metadata: Elicitation.Metadata<String, *>) =
        ElicitationModelStringReadWriteProperty<String>(
            data::get,
            data::set,
            title,
            description,
            true,
            metadata.toList()
        )

    fun optionalString(title: String, description: String, vararg metadata: Elicitation.Metadata<String, *>) =
        ElicitationModelStringReadWriteProperty<String?>(
            data::get,
            data::set,
            title,
            description,
            false,
            metadata.toList()
        )

    override fun equals(other: Any?) = when {
        this === other -> true
        other !is ElicitationModel -> false
        data != other.data -> false
        else -> true
    }

    override fun hashCode() = data.hashCode()
}
//
//    fun long(title: String, description: String, vararg metadata: Elicitation.Metadata<Long, *>) = object : ReadWriteProperty<ElicitationModel, Long> {
//        override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
//            (thisRef.asJsonObject() as MoshiObject)[property.name]!!
//                .let { McpJson.integer(it) }
//
//        override fun setValue(
//            thisRef: ElicitationModel,
//            property: KProperty<*>,
//            value: Long
//        ) {
//            TODO("Not yet implemented")
//        }
//    }
//
//    fun optionalLong(title: String, description: String, vararg metadata: Elicitation.Metadata<Long, *>) =
//        object : ReadWriteProperty<ElicitationModel, Long?> {
//            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
//                (thisRef.asJsonObject() as MoshiObject)[property.name]
//                    ?.let { McpJson.integer(it) }
//
//            override fun setValue(
//                thisRef: ElicitationModel,
//                property: KProperty<*>,
//                value: Long?
//            ) {
//                TODO("Not yet implemented")
//            }
//        }
//
//    fun integer(title: String, description: String, vararg metadata: Elicitation.Metadata<Int, *>) =
//        object : ReadWriteProperty<ElicitationModel, Int> {
//            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
//                (thisRef.asJsonObject() as MoshiObject)[property.name]!!
//                    .let { McpJson.integer(it) }
//                    .toInt()
//
//            override fun setValue(
//                thisRef: ElicitationModel,
//                property: KProperty<*>,
//                value: Int
//            ) {
//                TODO("Not yet implemented")
//            }
//        }
//
//    fun optionalIntegar(title: String, description: String, vararg metadata: Elicitation.Metadata<Int, *>) =
//        object : ReadWriteProperty<ElicitationModel, Int?> {
//            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
//                (thisRef.asJsonObject() as MoshiObject)[property.name]
//                    ?.let { McpJson.integer(it) }
//                    ?.toInt()
//
//            override fun setValue(
//                thisRef: ElicitationModel,
//                property: KProperty<*>,
//                value: Int?
//            ) {
//                TODO("Not yet implemented")
//            }
//        }
//
//    fun boolean(title: String, description: String, vararg metadata: Elicitation.Metadata<Boolean, *>) =
//        object : ReadWriteProperty<ElicitationModel, Boolean> {
//            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
//                (thisRef.asJsonObject() as MoshiObject)[property.name]!!
//                    .let { McpJson.bool(it) }
//
//            override fun setValue(
//                thisRef: ElicitationModel,
//                property: KProperty<*>,
//                value: Boolean
//            ) {
//                TODO("Not yet implemented")
//            }
//        }
//
//    fun optionalBoolean(title: String, description: String, vararg metadata: Elicitation.Metadata<Boolean, *>) =
//        object : ReadWriteProperty<ElicitationModel, Boolean?> {
//            override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) =
//                (thisRef.asJsonObject() as MoshiObject)[property.name]
//                    ?.let { McpJson.bool(it) }
//
//            override fun setValue(
//                thisRef: ElicitationModel,
//                property: KProperty<*>,
//                value: Boolean?
//            ) {
//                TODO("Not yet implemented")
//            }
//        }

class ElicitationModelStringReadWriteProperty<T>(
    private val get: (String) -> Any?,
    private val set: (String, Any?) -> Unit,
    val title: String,
    val description: String,
    val required: Boolean,
    val metadata: List<Elicitation.Metadata<String, *>>
) : ReadWriteProperty<ElicitationModel, T> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) = get(property.name) as T

    override fun setValue(thisRef: ElicitationModel, property: KProperty<*>, value: T) {
        set(property.name, value)
    }
}
