package org.http4k.lens

interface LensInjector<in IN, in OUT> {
    /**
     * Lens operation to set the value into the target
     */
    operator fun <R : OUT> invoke(value: IN, target: R): R

    /**
     * Lens operation to set the value into the target. Synomym for invoke(IN, OUT)
     */
    fun <R : OUT> inject(value: IN, target: R): R = invoke(value, target)

    /**
     * Lens operation to set the value into the target. Synomym for invoke(IN, OUT)
     */
    operator fun <R : OUT> set(target: R, value: IN) = inject(value, target)

    /**
     * Bind this Lens to a value, so we can set it into a target
     */
    infix fun <R : OUT> of(value: IN): (R) -> R = { invoke(value, it) }

    /**
     * Restrict the type that this Lens can inject into
     */
    fun <NEXT : OUT> restrictInto(): LensInjector<IN, NEXT> = this
}
