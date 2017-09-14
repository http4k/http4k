package org.http4k.lens

interface LensInjector<in IN, in OUT> {
    /**
     * Lens operation to set the value into the target
     */
    operator fun <R : IN> invoke(value: OUT, target: R): R

    /**
     * Lens operation to set the value into the target. Synomym for invoke(OUT, IN)
     */
    fun <R : IN> inject(value: OUT, target: R): R = invoke(value, target)

    /**
     * Lens operation to set the value into the target. Synomym for invoke(OUT, IN)
     */
    operator fun <R : IN> set(target: R, value: OUT) = inject(value, target)

    /**
     * Bind this Lens to a value, so we can set it into a target
     */
    infix fun <R : IN> of(value: OUT): (R) -> R = { invoke(value, it) }
}