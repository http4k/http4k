package org.reekwest.http.contract

class Get<in IN, MID, out OUT> private constructor(private val rootFn: (String, IN) -> List<MID>, private val fn: (MID) -> OUT) {
    operator fun invoke(name: String) = { target: IN -> rootFn(name, target).map(fn) }

    fun <NEXT> map(nextFn: (OUT) -> NEXT) = Get(rootFn, { nextFn(fn(it)) })

    companion object {
        operator fun <IN, OUT> invoke(rootFn: (String, IN) -> List<OUT>): Get<IN, OUT, OUT> = Get(rootFn, { it })
    }
}

class Set<IN, MID, in OUT> private constructor(private val rootFn: (String, List<MID>, IN) -> IN, private val fn: (OUT) -> MID) {
    operator fun invoke(name: String) = { values: List<OUT>, target: IN -> rootFn(name, values.map(fn), target) }
    fun <NEXT> map(nextFn: (NEXT) -> OUT) = Set(rootFn, { value: NEXT -> fn(nextFn(value)) })

    companion object {
        operator fun <IN, OUT> invoke(rootFn: (String, List<OUT>, IN) -> IN): Set<IN, OUT, OUT> = Set(rootFn, { it })
    }
}

open class Lens<in IN, out FINAL>(val meta: Meta,
                                  private val get: (IN) -> FINAL) : (IN) -> FINAL {
    override fun toString(): String = "${if (meta.required) "Required" else "Optional"} ${meta.location} '${meta.name}'"

    /**
     * Lens operation to get the value from the target
     */
    override operator fun invoke(target: IN): FINAL = try {
        get(target)
    } catch (e: ContractBreach) {
        throw e
    } catch (e: Exception) {
        throw ContractBreach(Invalid(this))
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
