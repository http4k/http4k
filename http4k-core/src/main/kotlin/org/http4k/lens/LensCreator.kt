package org.http4k.lens

interface LensCreator<in IN, out OUT> {
    /**
     * Lens operation to create a target from the value.
     */
    operator fun invoke(target: IN): OUT

    /**
     * Lens operation to create a target from the value. Synonym for invoke(IN)
     */
    fun create(value: IN): OUT = invoke(value)
}