package org.http4k.lens

/**
 * A Lens provides the uni-directional extraction of an entity from a target.
 */
open class Lens<in IN, out FINAL>(val meta: Meta,
                                  private val get: (IN) -> FINAL) : LensExtractor<IN, FINAL>, Iterable<Meta> {
    override fun iterator(): Iterator<Meta> = listOf(meta).iterator()

    override fun toString(): String = "${if (meta.required) "Required" else "Optional"} ${meta.location} '${meta.name}'"

    override operator fun invoke(target: IN): FINAL = try {
        get(target)
    } catch (e: LensFailure) {
        throw e
    } catch (e: Exception) {
        throw LensFailure(invalid())
    }
}

/**
 * A BiDiLens provides the bi-directional extraction of an entity from a target, or the insertion of an entity
 * into a target.
 */
class BiDiLens<in IN, FINAL>(meta: Meta,
                             get: (IN) -> FINAL,
                             private val set: (FINAL, IN) -> IN) : LensInjector<IN, FINAL>, Lens<IN, FINAL>(meta, get) {

    @Suppress("UNCHECKED_CAST")
    override operator fun <R : IN> invoke(value: FINAL, target: R): R = set(value, target) as R
}
