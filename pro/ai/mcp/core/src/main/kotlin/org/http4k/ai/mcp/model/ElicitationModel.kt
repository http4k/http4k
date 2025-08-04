package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.util.McpJson
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Superclass for all elicitation models. Creates a JSON schema for the model and provides
 * properties for defining the model's fields.
 */
abstract class ElicitationModel {

    private val data = mutableMapOf<String, Any?>()

    fun string(title: String, description: String, vararg metadata: Elicitation.Metadata<String, *>) =
        required(title, description, StringParam, metadata)

    fun optionalString(title: String, description: String, vararg metadata: Elicitation.Metadata<String, *>) =
        optional(title, description, StringParam, metadata)

    fun int(title: String, description: String, vararg metadata: Elicitation.Metadata<Int, *>) =
        required(title, description, IntegerParam, metadata)

    fun optionalInt(title: String, description: String, vararg metadata: Elicitation.Metadata<Long, *>) =
        optional(title, description, IntegerParam, metadata)

    fun long(title: String, description: String, vararg metadata: Elicitation.Metadata<Long, *>) =
        required(title, description, IntegerParam, metadata)

    fun optionalLong(title: String, description: String, vararg metadata: Elicitation.Metadata<Long, *>) =
        optional(title, description, IntegerParam, metadata)

    fun double(title: String, description: String, vararg metadata: Elicitation.Metadata<Double, *>) =
        required(title, description, NumberParam, metadata)

    fun optionalDouble(title: String, description: String, vararg metadata: Elicitation.Metadata<Double, *>) =
        optional(title, description, NumberParam, metadata)

    fun <T : Enum<T>> enum(
        title: String,
        description: String,
        nameOverrides: Elicitation.Metadata.EnumNames<T>? = null
    ) = required(title, description, StringParam, nameOverrides?.let { arrayOf(it) } ?: emptyArray())

    fun <T : Enum<T>> optionalEnum(
        title: String,
        description: String,
        nameOverrides: Elicitation.Metadata.EnumNames<T>? = null
    ) = optional(title, description, StringParam, nameOverrides?.let { arrayOf(it) } ?: emptyArray())

    fun boolean(title: String, description: String, vararg metadata: Elicitation.Metadata<Boolean, *>) =
        required(title, description, BooleanParam, metadata)

    fun optionalBoolean(title: String, description: String, vararg metadata: Elicitation.Metadata<Boolean, *>) =
        optional(title, description, BooleanParam, metadata)

    private fun <T> required(
        title: String,
        description: String,
        meta: ParamMeta,
        metadata: Array<out Elicitation.Metadata<T, *>>
    ): ElicitationModelStringReadWriteProperty<T> = ElicitationModelStringReadWriteProperty(
        data::get,
        data::set,
        title,
        description,
        meta,
        true,
        metadata.toList()
    )

    private fun <T> optional(
        title: String,
        description: String,
        meta: ParamMeta,
        metadata: Array<out Elicitation.Metadata<T, *>>
    ): ElicitationModelStringReadWriteProperty<T> = ElicitationModelStringReadWriteProperty(
        data::get,
        data::set,
        title,
        description,
        meta,
        false,
        metadata.toList()
    )

    private fun properties() =
        (this::class as KClass<ElicitationModel>).memberProperties
            .mapNotNull { p ->
                p.isAccessible = true
                (p.getDelegate(this) as? ElicitationModelStringReadWriteProperty<*>)
                    ?.let { p.name to it }
            }.toMap()

    override fun toString() = (this::class.simpleName + "(" +
        properties().map { (k, _) -> "$k=${data[k]}" }.joinToString(", ") + ")")

    internal fun toSchema() =
        McpJson {
            obj(
                "type" to string("object"),
                "required" to array(properties().filter { it.value.required }.map { string(it.key) }),
                "properties" to obj(
                    properties()
                        .map {
                            it.key to obj(
                                listOf(
                                    "type" to string(it.value.type.description),
                                    "description" to string(it.value.description),
                                    "title" to string(it.value.title),
                                ) + it.value.metadata.flatMap {
                                    it.data().map { it.first to McpJson.asJsonObject(it.second) }
                                },
                            )
                        }
                )
            )
        }

    override fun equals(other: Any?) = when {
        this === other -> true
        other !is ElicitationModel -> false
        data != other.data -> false
        else -> true
    }

    override fun hashCode() = data.hashCode()

}

class ElicitationModelStringReadWriteProperty<T>(
    private val get: (String) -> Any?,
    private val set: (String, Any?) -> Unit,
    val title: String,
    val description: String,
    val type: ParamMeta,
    val required: Boolean,
    val metadata: List<Elicitation.Metadata<in T, *>>
) : ReadWriteProperty<ElicitationModel, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) = get(property.name) as T

    override fun setValue(thisRef: ElicitationModel, property: KProperty<*>, value: T) = set(property.name, value)
}
