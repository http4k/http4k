package org.reekwest.http.lens

data class Meta(val required: Boolean, val location: String, val paramMeta: ParamMeta, val name: String, val description: String? = null)

open class Lens<in IN, out FINAL>(val meta: Meta,
                                  private val get: (IN) -> FINAL) : (IN) -> FINAL, Iterable<Meta> {
    override fun iterator(): Iterator<Meta> = listOf(meta).iterator()

    override fun toString(): String = "${if (meta.required) "Required" else "Optional"} ${meta.location} '${meta.name}'"

    /**
     * Lens operation to get the value from the target
     * @throws LensFailure if the value could not be retrieved from the target (missing/invalid etc)
     */
    @Throws(LensFailure::class)
    override operator fun invoke(target: IN): FINAL = try {
        get(target)
    } catch (e: LensFailure) {
        throw e
    } catch (e: Exception) {
        throw LensFailure(invalid())
    }
}

class BiDiLens<in IN, FINAL>(meta: Meta,
                             get: (IN) -> FINAL,
                             private val set: (FINAL, IN) -> IN) : Lens<IN, FINAL>(meta, get) {

    /**
     * Lens operation to set the value into the target
     *
     * The arguments to this method are in this specific order so we can partially apply several functions
     * and then fold them over a single target to modify.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <R : IN> invoke(value: FINAL, target: R): R = set(value, target) as R

    /**
     * Bind this Lens to a value, so we can set it into a target
     */
    infix fun <R : IN> to(value: FINAL): (R) -> R = { invoke(value, it) }
}
