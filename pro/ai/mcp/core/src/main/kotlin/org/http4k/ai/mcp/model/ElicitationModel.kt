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
 *
 * Extend this class to create a custom elicitation model and define properties via delegation.
 *
 * Note: Requires Kotlin-reflect dependency to introspect properties.
 */
abstract class ElicitationModel {

    private val data = mutableMapOf<String, Any?>()

    fun string(
        title: String,
        description: String,
        default: String? = null,
        vararg metadata: Elicitation.Metadata<String, *>
    ) =
        required(title, description, StringParam, default, metadata) { it }

    fun optionalString(
        title: String,
        description: String,
        default: String? = null,
        vararg metadata: Elicitation.Metadata<String, *>
    ) =
        optional(title, description, StringParam, default, metadata) { it }

    fun int(
        title: String,
        description: String,
        default: Int? = null,
        vararg metadata: Elicitation.Metadata<Int, *>
    ) =
        required(title, description, IntegerParam, default, metadata) { it }

    fun optionalInt(
        title: String,
        description: String,
        default: Int? = null,
        vararg metadata: Elicitation.Metadata<Int, *>
    ) =
        optional(title, description, IntegerParam, default, metadata) { it }

    fun long(
        title: String,
        description: String,
        default: Long? = null,
        vararg metadata: Elicitation.Metadata<Long, *>
    ) =
        required(title, description, IntegerParam, default, metadata) { it }

    fun optionalLong(
        title: String,
        description: String,
        default: Long? = null,
        vararg metadata: Elicitation.Metadata<Long, *>
    ) =
        optional(title, description, IntegerParam, default, metadata) { it }

    fun double(
        title: String,
        description: String,
        default: Double? = null,
        vararg metadata: Elicitation.Metadata<Double, *>
    ) =
        required(title, description, NumberParam, default, metadata) { it }

    fun optionalDouble(
        title: String,
        description: String,
        default: Double? = null,
        vararg metadata: Elicitation.Metadata<Double, *>
    ) = optional(title, description, NumberParam, default, metadata) { it }

    inline fun <reified T : Enum<T>> enum(
        title: String,
        description: String,
        nameOverrides: Elicitation.Metadata.EnumNames<T>,
        default: T? = null,
    ): ElicitationModelStringReadWriteProperty<T> = enumWithValues(title, description, default, nameOverrides)

    fun <T : Enum<T>> enumWithValues(
        title: String,
        description: String,
        default: T? = null,
        nameOverrides: Elicitation.Metadata.EnumNames<T>? = null
    ): ElicitationModelStringReadWriteProperty<T> = required(
        title,
        description,
        StringParam,
        default,
        nameOverrides?.let { arrayOf(it) } ?: emptyArray()) { it }

    fun <T : Enum<T>> optionalEnum(
        title: String,
        description: String,
        default: T? = null,
        nameOverrides: Elicitation.Metadata.EnumNames<T>? = null
    ) = optional(title, description, StringParam, default, nameOverrides?.let { arrayOf(it) } ?: emptyArray()) { it }

    fun boolean(
        title: String, description: String,
        default: Boolean? = null,
        vararg metadata: Elicitation.Metadata<Boolean, *>
    ) = required(title, description, BooleanParam, default, metadata) { it }

    fun optionalBoolean(
        title: String, description: String,
        default: Boolean? = null,
        vararg metadata: Elicitation.Metadata<Boolean, *>
    ) = optional(title, description, BooleanParam, default, metadata) { it }

    private fun <OUT, IN> required(
        title: String,
        description: String,
        meta: ParamMeta,
        default: Any?,
        metadata: Array<out Elicitation.Metadata<OUT, *>>,
        map: (OUT) -> IN
    ): ElicitationModelStringReadWriteProperty<OUT> = ElicitationModelStringReadWriteProperty(
        data::get,
        { key, value -> data[key] = value?.let { map(it) } },
        title,
        description,
        meta,
        true,
        default,
        metadata.toList()
    )

    private fun <IN, OUT> optional(
        title: String,
        description: String,
        meta: ParamMeta,
        default: Any?,
        metadata: Array<out Elicitation.Metadata<OUT, *>>,
        map: (OUT) -> IN
    ): ElicitationModelStringReadWriteProperty<OUT> = ElicitationModelStringReadWriteProperty(
        data::get,
        { key, value -> data[key] = value?.let { map(it) } },
        title,
        description,
        meta,
        false,
        default,
        metadata.toList()
    )

    private fun properties() =
        @Suppress("UNCHECKED_CAST")
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
                                    "default" to (it.value.default?.let { McpJson.asJsonObject(it) }
                                        ?: McpJson.nullNode()),
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
    private val set: (String, T?) -> Unit,
    val title: String,
    val description: String,
    val type: ParamMeta,
    val required: Boolean,
    val default: Any?,
    val metadata: List<Elicitation.Metadata<in T, *>>
) : ReadWriteProperty<ElicitationModel, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: ElicitationModel, property: KProperty<*>) = (get(property.name) ?: default) as T
    override fun setValue(thisRef: ElicitationModel, property: KProperty<*>, value: T) = set(property.name, value)
}
