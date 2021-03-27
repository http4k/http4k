package org.http4k.format

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.lens.BiDiMapping
import java.math.BigDecimal
import java.math.BigInteger

@JvmName("stringMapper")
inline fun <BUILD, reified VALUE : Value<T>, T : Any> AutoMappingConfiguration<BUILD>.value(fn: ValueFactory<VALUE, T>) =
    text(BiDiMapping(fn::parse, fn::show))

@JvmName("booleanMapper")
inline fun <BUILD, reified VALUE : Value<Boolean>> AutoMappingConfiguration<BUILD>.value(fn: ValueFactory<VALUE, Boolean>) =
    boolean(BiDiMapping(fn::of) { it.value })

@JvmName("intMapper")
inline fun <BUILD, reified VALUE : Value<Int>> AutoMappingConfiguration<BUILD>.value(fn: ValueFactory<VALUE, Int>) =
    int(BiDiMapping(fn::of) { it.value })

@JvmName("longMapper")
inline fun <BUILD, reified VALUE : Value<Long>> AutoMappingConfiguration<BUILD>.value(fn: ValueFactory<VALUE, Long>) =
    long(BiDiMapping(fn::of) { it.value })

@JvmName("doubleMapper")
inline fun <BUILD, reified VALUE : Value<Double>> AutoMappingConfiguration<BUILD>.value(fn: ValueFactory<VALUE, Double>) =
    double(BiDiMapping(fn::of) { it.value })

@JvmName("bigDecimal")
inline fun <BUILD, reified VALUE : Value<BigDecimal>> AutoMappingConfiguration<BUILD>.value(fn: ValueFactory<VALUE, BigDecimal>) =
    bigDecimal(BiDiMapping(fn::of) { it.value })

@JvmName("bigIntegerMapper")
inline fun <BUILD, reified VALUE : Value<BigInteger>> AutoMappingConfiguration<BUILD>.value(fn: ValueFactory<VALUE, BigInteger>) =
    bigInteger(BiDiMapping(fn::of) { it.value })
