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

interface LensExtractor<in IN, out OUT> : (IN) -> OUT {
    /**
     * Lens operation to get the value from the target
     * @throws LensFailure if the value could not be retrieved from the target (missing/invalid etc)
     */
    @Throws(LensFailure::class)
    override operator fun invoke(target: IN): OUT

    /**
     * Lens operation to get the value from the target. Synonym for invoke(IN)
     * @throws LensFailure if the value could not be retrieved from the target (missing/invalid etc)
     */
    @Throws(LensFailure::class)
    fun extract(target: IN): OUT = invoke(target)

    /**
     * Lens operation to get the value from the target. Synonym for invoke(IN)
     */
    operator fun <R : IN> get(target: R) = extract(target)

    /**
     * Restrict the type that this Lens can extract from
     */
    fun <NEXT : IN> restrictFrom(): LensExtractor<NEXT, OUT> = this
}

interface LensInjectorExtractor<in IN, OUT> : LensExtractor<IN, OUT>, LensInjector<OUT, IN>
