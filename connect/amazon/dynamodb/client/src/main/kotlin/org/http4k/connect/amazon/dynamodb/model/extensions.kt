package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.connect.amazon.dynamodb.model.DynamoDataType.valueOf
import org.http4k.lens.BiDiMapping

typealias TokensToNames = Map<String, AttributeName>
typealias TokensToValues = Map<String, AttributeValue>
typealias ItemResult = Map<String, Map<String, Any>>

fun ItemResult.toItem() =
    map {
        val (key, v) = it.value.entries.first()
        AttributeName.of(it.key) to AttributeValue.from(valueOf(key), v)
    }
        .toMap()

/**
 * Used for creating tables
 */
fun <T> Attribute<T>.asKeySchema(keyType: KeyType) = KeySchema(name, keyType)

/**
 * Used for creating tables
 */
fun <T> Attribute<T>.asAttributeDefinition() = AttributeDefinition(name, dataType)

/**
 * Map items out of a collection
 */
fun <T> Attribute.AttrLensSpec<List<AttributeValue>>.map(next: BiDiMapping<Item, T>): Attribute.AttrLensSpec<List<T>> =
    map(
        { it.mapNotNull { it.M }.map { next(it) } },
        { it.map { AttributeValue.Map(next(it)) } },
    )

/**
 * Map items out of a collection
 */
fun <T> Attribute.Companion.list(next: BiDiMapping<Item, T>) = list().map(
    { it.mapNotNull { it.M }.map { next(it) } },
    { it.map { AttributeValue.Map(next(it)) } },
)

fun <T> Attribute.Companion.map(next: BiDiMapping<Item, T>) = map().map(next)

/**
 * Map a single value type to an attribute
 */
fun <P : Any, VALUE : Value<P>> Attribute.AttrLensSpec<P>.value(vf: ValueFactory<VALUE, P>) = map(vf::of, vf::unwrap)

/**
 * Map a list of value types to an attribute
 */
fun <T : Any, VALUE : Value<T>> Attribute.Companion.list(vf: ValueFactory<VALUE, T>) =
    vf.asList({ it.S?.let(vf::parse)?.value }, { AttributeValue.Str(it?.let(vf::of)?.let(vf::show)) })

/**
 * Map a set of value types to an attribute
 */
fun <T : Any, VALUE : Value<T>> Attribute.Companion.set(vf: ValueFactory<VALUE, T>) = strings()
    .map({ it.map { it.let(vf::parse) }.toSet() }, { it.map(vf::show).toSet() })


internal fun <P : Any, VALUE : Value<P>> ValueFactory<VALUE, P>.asList(
    nextIn: (AttributeValue) -> P?,
    nextOut: (P?) -> AttributeValue
) = Attribute.list().map({ it.mapNotNull(nextIn).map(::of) }, { it.map(::unwrap).map(nextOut) })

internal fun <P : Number, VALUE : Value<P>> Attribute.AttrLensSpec<Set<P>>.asSet(vf: ValueFactory<VALUE, P>) =
    map({ it.map(vf::of).toSet() }, { it.map(vf::unwrap).toSet() })
