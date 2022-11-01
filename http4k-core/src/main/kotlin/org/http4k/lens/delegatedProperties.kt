package org.http4k.lens

import kotlin.properties.ReadOnlyProperty

typealias Prop<OUT> = ReadOnlyProperty<Any?, OUT>

interface DelegatedPropertyLensSpec<Req, OUT, Opt> {
    fun required(): Prop<Req>
    fun optional(): Prop<Opt>
    fun defaulted(default: OUT): Prop<Req>
}

@JvmName("named")
fun <IN : Any, OUT, L : LensBuilder<IN, OUT>> L.by() =
    object : DelegatedPropertyLensSpec<Lens<IN, OUT>, OUT, Lens<IN, OUT?>> {
        override fun required() = Prop { _, p -> this@by.required(p.name) }

        override fun optional() = Prop { _, p -> this@by.optional(p.name) }

        override fun defaulted(default: OUT) = Prop { _, property -> this@by.defaulted(property.name, default) }
    }

@JvmName("namedList")
fun <IN : Any, OUT, L : LensBuilder<IN, List<OUT>>> L.by() =
    object : DelegatedPropertyLensSpec<Lens<IN, List<OUT>>, List<OUT>, Lens<IN, List<OUT>?>> {
        override fun required() = Prop { _, p -> this@by.required(p.name) }

        override fun optional() = Prop { _, p -> this@by.optional(p.name) }

        override fun defaulted(default: List<OUT>) = Prop { _, p -> this@by.defaulted(p.name, default) }
    }

@JvmName("namedBiDi")
fun <IN : Any, OUT, L : BiDiLensBuilder<IN, OUT>> L.by() =
    object : DelegatedPropertyLensSpec<BiDiLens<IN, OUT>, OUT, BiDiLens<IN, OUT?>> {
        override fun required() = Prop { _, p -> this@by.required(p.name) }

        override fun optional() = Prop { _, p -> this@by.optional(p.name) }

        override fun defaulted(default: OUT) = Prop { _, p -> this@by.defaulted(p.name, default) }
    }

@JvmName("namedBiDiList")
fun <IN : Any, OUT, L : BiDiLensBuilder<IN, List<OUT>>> L.by() =
    object : DelegatedPropertyLensSpec<BiDiLens<IN, List<OUT>>, List<OUT>, BiDiLens<IN, List<OUT>?>> {
        override fun required() = Prop { _, p -> this@by.required(p.name) }

        override fun optional() = Prop { _, p -> this@by.optional(p.name) }

        override fun defaulted(default: List<OUT>) = Prop { _, p -> this@by.defaulted(p.name, default) }
    }
