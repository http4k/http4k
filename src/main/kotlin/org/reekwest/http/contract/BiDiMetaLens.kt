package org.reekwest.http.contract

abstract class MetaLens<in IN, in OUT, out FINAL>(val meta: Meta, private val delegateLens: Get<IN, OUT>) {
    override fun toString(): String = "${if (meta.required) "Required" else "Optional"} ${meta.location} '${meta.name}'"

    /**
     * Lens operation to get the value from the target
     */
    operator fun invoke(target: IN): FINAL = try {
        convertIn(delegateLens(target))
    } catch (e: ContractBreach) {
        throw e
    } catch (e: Exception) {
        throw ContractBreach.Invalid(this)
    }

    abstract internal fun convertIn(o: List<OUT>): FINAL
}

abstract class BiDiMetaLens<in IN, OUT, FINAL>(meta: Meta, get: (IN) -> List<OUT>, private val set: (List<OUT>, IN) -> IN
) : MetaLens<IN, OUT, FINAL>(meta, get) {

    /**
     * Lens operation to set the value into the target
     *
     * The arguments to this method are in this specific order so we can partially apply several functions
     * and then fold them over a single target to modify.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <R : IN> invoke(value: FINAL, target: R): R = set(convertOut(value), target) as R

    /**
     * Bind this Lens to a value, so we can set it into a target
     */
    infix fun <R : IN> to(value: FINAL): (R) -> R = { invoke(value, it) }

    abstract internal fun convertOut(o: FINAL): List<OUT>
}
