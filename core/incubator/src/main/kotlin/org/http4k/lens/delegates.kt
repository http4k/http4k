package org.http4k.lens

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

sealed interface TypedField<IN : HttpMessage, OUT : Any> {

    class Required<IN : HttpMessage, OUT : Any>(internal val spec: BiDiLensBuilder<IN, OUT>) :
        ReadWriteProperty<IN, OUT>, TypedField<IN, OUT> {
        override fun getValue(thisRef: IN, property: KProperty<*>) = spec.required(property.name)(thisRef)

        override fun setValue(thisRef: IN, property: KProperty<*>, value: OUT) {
            spec.required(property.name)(value, thisRef)
        }
    }

    class Optional<IN : HttpMessage, OUT : Any>(internal val spec: BiDiLensBuilder<IN, OUT>) :
        ReadWriteProperty<IN, OUT?>, TypedField<IN, OUT> {
        override fun getValue(thisRef: IN, property: KProperty<*>) = spec.optional(property.name)(thisRef)

        override fun setValue(thisRef: IN, property: KProperty<*>, value: OUT?) {
            spec.optional(property.name)(value, thisRef)
        }
    }

    class Defaulted<IN : HttpMessage, OUT : Any>(
        internal val spec: BiDiLensBuilder<IN, OUT>,
        private val default: (IN) -> OUT
    ) : ReadWriteProperty<IN, OUT>, TypedField<IN, OUT> {
        override fun getValue(thisRef: IN, property: KProperty<*>) = spec.defaulted(property.name, default)(thisRef)

        override fun setValue(thisRef: IN, property: KProperty<*>, value: OUT) {
            spec.optional(property.name)(value, thisRef)
        }
    }

    class Body<IN : HttpMessage, OUT : Any>(internal val spec: BiDiBodyLensSpec<OUT>, val example: OUT?) :
        ReadWriteProperty<IN, OUT>, TypedField<IN, OUT> {
        override fun getValue(thisRef: IN, property: KProperty<*>) = spec.toLens()(thisRef)

        override fun setValue(thisRef: IN, property: KProperty<*>, value: OUT) {
            spec.toLens()(value, thisRef)
        }
    }

    class Path<OUT: Any>(internal val spec: PathLensSpec<OUT>) : ReadOnlyProperty<Request, OUT>, TypedField<Request, OUT> {
        override fun getValue(thisRef: Request, property: KProperty<*>): OUT {
            return spec.of(property.name)(thisRef)
        }
    }
}
