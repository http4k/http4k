package org.reekwest.http.contract

class GetLens<in IN, MID, out OUT>(private val rootFn: (String, IN) -> List<MID>, private val fn: (MID) -> OUT) {
    operator fun invoke(name: String) = { target: IN -> rootFn(name, target).map(fn) }

    fun <NEXT> map(nextFn: (OUT) -> NEXT) = GetLens(rootFn, { nextFn(fn(it)) })
}

class SetLens<IN, MID, in OUT>(private val rootFn: (String, List<MID>, IN) -> IN, private val fn: (OUT) -> MID) {
    operator fun invoke(name: String) = { values: List<OUT>, target: IN -> rootFn(name, values.map(fn), target) }
    fun <NEXT> map(nextFn: (NEXT) -> OUT) = SetLens(rootFn, { value: NEXT -> fn(nextFn(value)) })
}


abstract class Lens<in IN, in OUT, out FINAL>(val meta: Meta, private val get: (IN) -> List<OUT>) {
    override fun toString(): String = "${if (meta.required) "Required" else "Optional"} ${meta.location} '${meta.name}'"

    /**
     * Lens operation to get the value from the target
     */
    operator fun invoke(target: IN): FINAL = try {
        convertIn(get(target))
    } catch (e: ContractBreach) {
        throw e
    } catch (e: Exception) {
        throw ContractBreach.Invalid(this)
    }

    abstract internal fun convertIn(o: List<OUT>): FINAL
}

abstract class BiDiLens<in IN, OUT, FINAL>(meta: Meta, get: (IN) -> List<OUT>,
                                           private val set: (List<OUT>, IN) -> IN ) : Lens<IN, OUT, FINAL>(meta, get) {

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
