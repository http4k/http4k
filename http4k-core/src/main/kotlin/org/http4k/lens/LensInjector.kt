package org.http4k.lens

interface LensInjector<in TARGET, in IN> {
    /**
     * Lens operation to set the value into the target
     */
    operator fun <R : TARGET> invoke(value: IN, target: R): R

    /**
     * Lens operation to set the value into the target. Synomym for invoke(OUT, IN)
     */
    fun <R : TARGET> inject(value: IN, target: R): R = invoke(value, target)

    /**
     * Lens operation to set the value into the target. Synomym for invoke(OUT, IN)
     */
    operator fun <R : TARGET> set(target: R, value: IN) = inject(value, target)

    /**
     * Bind this Lens to a value, so we can set it into a target
     */
    infix fun <R : TARGET> of(value: IN): (R) -> R = { invoke(value, it) }
}