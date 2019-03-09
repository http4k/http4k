package org.http4k.lens

/**
 * Runs through a list of lenses and extracts the values from each one, collecting the results
 */
enum class Validator(private val actOn: (List<Failure>) -> List<Failure>) {
    Strict({ if (it.isNotEmpty()) throw LensFailure(it, target = null) else it }),
    Feedback({ it }),
    Ignore({ emptyList<Failure>() }),
    None({ emptyList<Failure>() }) {
        override fun <T : Any> invoke(entity: T, vararg lenses: Lens<T, *>) = emptyList<Failure>()
    };

    open operator fun <T : Any> invoke(entity: T, vararg lenses: Lens<T, *>): List<Failure> =
        lenses.fold(emptyList<Failure>()) { memo, next ->
            try {
                next(entity)
                memo
            } catch (e: LensFailure) {
                memo.plus(e.failures)
            }
        }.let(actOn)
}