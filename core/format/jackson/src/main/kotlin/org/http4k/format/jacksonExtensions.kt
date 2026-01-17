package org.http4k.format

import dev.forkhandles.data.JsonNodeDataContainer
import dev.forkhandles.values.Value
import org.http4k.core.Body
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiMapping
import org.http4k.lens.ContentNegotiation
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.KeyDeserializer
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.cfg.MapperBuilder
import tools.jackson.module.kotlin.KotlinModule
import java.math.BigDecimal
import java.math.BigInteger

fun <T : ObjectMapper> KotlinModule.asConfigurable(builder: MapperBuilder<T, *>): AutoMappingConfiguration<T> =
    object : AutoMappingConfiguration<T> {
        override fun <OUT> int(mapping: BiDiMapping<Int, OUT>) =
            adapter(mapping, JsonGenerator::writeNumber, JsonParser::getIntValue)

        override fun <OUT> long(mapping: BiDiMapping<Long, OUT>) =
            adapter(mapping, JsonGenerator::writeNumber, JsonParser::getLongValue)

        override fun <OUT> double(mapping: BiDiMapping<Double, OUT>) =
            adapter(mapping, JsonGenerator::writeNumber, JsonParser::getDoubleValue)

        override fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>) =
            adapter(mapping, JsonGenerator::writeNumber, JsonParser::getBigIntegerValue)

        override fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>) =
            adapter(mapping, JsonGenerator::writeNumber, JsonParser::getDecimalValue)

        override fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>) =
            adapter(mapping, JsonGenerator::writeBoolean, JsonParser::getBooleanValue)

        override fun <OUT> text(mapping: BiDiMapping<String, OUT>) =
            adapter(mapping, JsonGenerator::writeString, JsonParser::getValueAsString)
                .apply {
                    if (mapping.clazz.isEnum) {
                        addKeySerializer(mapping.clazz, object : ValueSerializer<OUT>() {
                            override fun serialize(value: OUT, gen: JsonGenerator, ctxt: SerializationContext) {
                                gen.writeName(mapping(value))
                            }
                        })
                        addKeyDeserializer(mapping.clazz, object : KeyDeserializer() {
                            override fun deserializeKey(key: String, ctxt: DeserializationContext) = mapping(key)
                        })
                    }
                }

        private fun <IN, OUT> adapter(
            mapping: BiDiMapping<IN, OUT>,
            write: JsonGenerator.(IN) -> Unit,
            read: JsonParser.() -> IN
        ) =
            apply {
                addDeserializer(mapping.clazz, object : ValueDeserializer<OUT>() {
                    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OUT =
                        mapping.invoke(p.read())
                })
                addSerializer(mapping.clazz, object : ValueSerializer<OUT>() {
                    override fun serialize(value: OUT, gen: JsonGenerator, ctxt: SerializationContext) =
                        gen.write(mapping(value))
                })
            }

        override fun done(): T = builder.apply { addModule(this@asConfigurable) }.build()
    }

/**
 * Prevent the unmarshalling of unknown values4k types.
 */
fun <BUILDER> AutoMappingConfiguration<BUILDER>.prohibitUnknownValues(): AutoMappingConfiguration<BUILDER> =
    text(
        BiDiMapping(
            Value::class.java,
            { error("no mapping for ${it.javaClass}") }, { error("no mapping for ${it.javaClass}") })
    )

/**
 * Custom lens to extract and inject Data4k DataContainer types from JSON bodies
 */
fun <T : JsonNodeDataContainer> Body.Companion.json(
    fn: (JsonNode) -> T,
    description: String? = null,
    contentNegotiation: ContentNegotiation = ContentNegotiation.None
): BiDiBodyLensSpec<T> = Jackson.body(description, contentNegotiation).map(fn, JsonNodeDataContainer::unwrap)

