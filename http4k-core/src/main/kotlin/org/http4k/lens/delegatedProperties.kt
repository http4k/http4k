package org.http4k.lens

import kotlin.reflect.KProperty

/**
 * Create pre-named lenses via property delegation.
 */
interface DelegatedPropertyLensBuilder<OUT> {
    operator fun <T> getValue(t: T, property: KProperty<*>): OUT
}

interface DelegatedPropertyLensSpec<Req, OUT, Opt> {
    fun required(): DelegatedPropertyLensBuilder<Req>
    fun optional(): DelegatedPropertyLensBuilder<Opt>
    fun defaulted(default: OUT): DelegatedPropertyLensBuilder<Req>
}

@JvmName("named")
fun <IN : Any, OUT, L : LensBuilder<IN, OUT>> L.by() =
    object : DelegatedPropertyLensSpec<Lens<IN, OUT>, OUT, Lens<IN, OUT?>> {
        override fun required() = object : DelegatedPropertyLensBuilder<Lens<IN, OUT>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.required(property.name)
        }

        override fun optional() = object : DelegatedPropertyLensBuilder<Lens<IN, OUT?>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.optional(property.name)
        }

        override fun defaulted(default: OUT) = object : DelegatedPropertyLensBuilder<Lens<IN, OUT>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.defaulted(property.name, default)
        }
    }

@JvmName("namedList")
fun <IN : Any, OUT, L : LensBuilder<IN, List<OUT>>> L.by() =
    object : DelegatedPropertyLensSpec<Lens<IN, List<OUT>>, List<OUT>, Lens<IN, List<OUT>?>> {
        override fun required() = object : DelegatedPropertyLensBuilder<Lens<IN, List<OUT>>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.required(property.name)
        }

        override fun optional() = object : DelegatedPropertyLensBuilder<Lens<IN, List<OUT>?>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.optional(property.name)
        }

        override fun defaulted(default: List<OUT>) = object : DelegatedPropertyLensBuilder<Lens<IN, List<OUT>>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.defaulted(property.name, default)
        }
    }

@JvmName("namedBiDi")
fun <IN : Any, OUT, L : BiDiLensBuilder<IN, OUT>> L.by() =
    object : DelegatedPropertyLensSpec<BiDiLens<IN, OUT>, OUT, BiDiLens<IN, OUT?>> {
        override fun required() = object : DelegatedPropertyLensBuilder<BiDiLens<IN, OUT>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.required(property.name)
        }

        override fun optional() = object : DelegatedPropertyLensBuilder<BiDiLens<IN, OUT?>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.optional(property.name)
        }

        override fun defaulted(default: OUT) = object : DelegatedPropertyLensBuilder<BiDiLens<IN, OUT>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.defaulted(property.name, default)
        }
    }

@JvmName("namedBiDiList")
fun <IN : Any, OUT, L : BiDiLensBuilder<IN, List<OUT>>> L.by() =
    object : DelegatedPropertyLensSpec<BiDiLens<IN, List<OUT>>, List<OUT>, BiDiLens<IN, List<OUT>?>> {
        override fun required() = object : DelegatedPropertyLensBuilder<BiDiLens<IN, List<OUT>>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.required(property.name)
        }

        override fun optional() = object : DelegatedPropertyLensBuilder<BiDiLens<IN, List<OUT>?>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.optional(property.name)
        }

        override fun defaulted(default: List<OUT>) = object : DelegatedPropertyLensBuilder<BiDiLens<IN, List<OUT>>> {
            override fun <T> getValue(t: T, property: KProperty<*>) = this@by.defaulted(property.name, default)
        }
    }
