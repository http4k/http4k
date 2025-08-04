package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.util.McpJson
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.StringParam
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

abstract class ElicitationModel {

    private val data = mutableMapOf<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    private fun properties() =
        (this::class as KClass<ElicitationModel>).memberProperties
            .mapNotNull { p ->
                p.isAccessible = true
                (p.getDelegate(this) as? ElicitationModelStringReadWriteProperty<*>)
                    ?.let { p.name to it }
            }.toMap()

    fun toSchema() =
        McpJson {
            obj(
                "type" to string("object"),
                "required" to array(properties().filter { it.value.required }.map { string(it.key) }),
                "properties" to obj(
                    properties()
                        .map {
                            it.key to obj(
                                "type" to string(it.value.type.description),
                                "description" to string(it.value.description),
                                "title" to string(it.value.title),
                            )
                        }
                )
            )

        }

    fun string(title: String, description: String, vararg metadata: Elicitation.Metadata<String, *>) =
        ElicitationModelStringReadWriteProperty<String>(
            data::get,
            data::set,
            title,
            description,
            StringParam,
            true,
            metadata.toList()
        )

    fun optionalString(title: String, description: String, vararg metadata: Elicitation.Metadata<String, *>) =
        ElicitationModelStringReadWriteProperty<String?>(
            data::get,
            data::set,
            title,
            description,
            StringParam,
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
    val type: ParamMeta,
    val required: Boolean,
    val metadata: List<Elicitation.Metadata<String, *>>
) : ReadWriteProperty<ElicitationModel, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) = get(property.name) as T

    override fun setValue(thisRef: ElicitationModel, property: KProperty<*>, value: T) = set(property.name, value)
}
