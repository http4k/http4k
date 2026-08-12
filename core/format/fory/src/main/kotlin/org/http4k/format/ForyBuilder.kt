package org.http4k.format

import org.apache.fory.Fory
import org.apache.fory.ThreadSafeFory
import org.apache.fory.config.Config
import org.apache.fory.context.ReadContext
import org.apache.fory.context.WriteContext
import org.apache.fory.serializer.Serializer
import org.http4k.lens.BiDiMapping
import java.math.BigDecimal
import java.math.BigInteger


fun ThreadSafeFory.asConfigurable() = object : AutoMappingConfiguration<ThreadSafeFory> {
    override fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>) =
        add(mapping, { writeBoolean(it) }, { readBoolean() })

    override fun <OUT> int(mapping: BiDiMapping<Int, OUT>) =
        add(mapping, { writeInt32(it) }, { readInt32() })

    override fun <OUT> long(mapping: BiDiMapping<Long, OUT>) =
        add(mapping, { writeInt64(it) }, { readInt64() })

    override fun <OUT> double(mapping: BiDiMapping<Double, OUT>) =
        add(mapping, { writeFloat64(it) }, { readFloat64() })

    override fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>) =
        add(mapping, { writeString(it.toString()) }, { BigInteger(readString()) })

    override fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>) =
        add(mapping, { writeString(it.toString()) }, { BigDecimal(readString()) })

    override fun <OUT> text(mapping: BiDiMapping<String, OUT>) =
        add(mapping, { writeString(it) }, { readString() })

    private fun <IN, OUT> add(
        mapping: BiDiMapping<IN, OUT>,
        write: WriteContext.(IN) -> Unit,
        read: ReadContext.() -> IN
    ) = apply {
        this@asConfigurable.registerCallback({ fory: Fory ->
            fory.registerSerializer(mapping.clazz, MappedSerializer(fory.config, mapping, write, read))
        })
    }

    override fun done() = this@asConfigurable
}

private class MappedSerializer<IN, OUT>(
    config: Config,
    private val mapping: BiDiMapping<IN, OUT>,
    private val write: WriteContext.(IN) -> Unit,
    private val read: ReadContext.() -> IN
) : Serializer<OUT>(config, mapping.clazz) {

    override fun write(context: WriteContext, value: OUT) = context.write(mapping(value))

    override fun read(context: ReadContext): OUT = mapping(context.read())
}
