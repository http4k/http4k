package org.reekwest.http.core.contract

abstract class Lens<in IN, OUT : Any, FINAL>(val meta: Meta, private val namedLens: NamedLens<IN, OUT>) {

    override fun toString(): String = "${if (meta.required) "Required" else "Optional"} ${meta.location} '${meta.name}'"

    /**
     * Lens operation to get the value from the target
     */
    operator fun invoke(target: IN): FINAL = try {
        convertIn(namedLens.get(target, meta.name))
    } catch (e: ContractBreach) {
        throw e
    } catch (e: Exception) {
        throw ContractBreach.Invalid(this)
    }

    /**
     * Lens operation to set the value into the target
     *
     * The arguments to this method are in this specific order so we can partially apply several functions
     * and then fold them over a single target to modify.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <R : IN> invoke(value: FINAL, target: R): R = namedLens.set(target, meta.name, convertOut(value)) as R

    /**
     * Bind this Lens to a value, so we can set it into a target
     */
    infix fun <R : IN> to(value: FINAL): (R) -> R = { invoke(value, it) }

    abstract internal fun convertIn(o: List<OUT?>?): FINAL
    abstract internal fun convertOut(o: FINAL): List<OUT>
}
