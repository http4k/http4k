package org.http4k.lens

import kotlin.reflect.KProperty

/**
 * Create pre-named lenses via property delegation.
 */
interface PropLens<OUT> {
    operator fun <T> getValue(t: T, property: KProperty<*>): OUT
}

interface NamedPropertyLens<Req, OUT, Opt> {
    fun required(): PropLens<Req>
    fun optional(): PropLens<Opt>
    fun defaulted(default: OUT): PropLens<Req>
}

@JvmName("named")
fun <IN : Any, OUT, L : LensBuilder<IN, OUT>> L.named() =
    object : NamedPropertyLens<Lens<IN, OUT>, OUT, Lens<IN, OUT?>> {
        override fun required() = object : PropLens<Lens<IN, OUT>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.required(property.name)
        }

        override fun optional() = object : PropLens<Lens<IN, OUT?>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.optional(property.name)
        }

        override fun defaulted(default: OUT) = object : PropLens<Lens<IN, OUT>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.defaulted(property.name, default)
        }
    }

@JvmName("namedList")
fun <IN : Any, OUT, L : LensBuilder<IN, List<OUT>>> L.named() =
    object : NamedPropertyLens<Lens<IN, List<OUT>>, List<OUT>, Lens<IN, List<OUT>?>> {
        override fun required() = object : PropLens<Lens<IN, List<OUT>>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.required(property.name)
        }

        override fun optional() = object : PropLens<Lens<IN, List<OUT>?>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.optional(property.name)
        }

        override fun defaulted(default: List<OUT>) = object : PropLens<Lens<IN, List<OUT>>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.defaulted(property.name, default)
        }
    }

@JvmName("namedBiDi")
fun <IN : Any, OUT, L : BiDiLensBuilder<IN, OUT>> L.named() =
    object : NamedPropertyLens<BiDiLens<IN, OUT>, OUT, BiDiLens<IN, OUT?>> {
        override fun required() = object : PropLens<BiDiLens<IN, OUT>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.required(property.name)
        }

        override fun optional() = object : PropLens<BiDiLens<IN, OUT?>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.optional(property.name)
        }

        override fun defaulted(default: OUT) = object : PropLens<BiDiLens<IN, OUT>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.defaulted(property.name, default)
        }
    }

@JvmName("namedBiDiList")
fun <IN : Any, OUT, L : BiDiLensBuilder<IN, List<OUT>>> L.named() =
    object : NamedPropertyLens<BiDiLens<IN, List<OUT>>, List<OUT>, BiDiLens<IN, List<OUT>?>> {
        override fun required() = object : PropLens<BiDiLens<IN, List<OUT>>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.required(property.name)
        }

        override fun optional() = object : PropLens<BiDiLens<IN, List<OUT>?>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.optional(property.name)
        }

        override fun defaulted(default: List<OUT>) = object : PropLens<BiDiLens<IN, List<OUT>>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@named.defaulted(property.name, default)
        }
    }
