package org.http4k.lens

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
