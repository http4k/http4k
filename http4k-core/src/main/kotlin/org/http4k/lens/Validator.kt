package org.http4k.lens

/**
 * Runs through a list of lenses and extracts the values from each one, collecting the errors
 */
enum class Validator(private val actOn: (LensFailure) -> List<Failure>) {
    Strict({ if (it.failures.isNotEmpty()) throw it else it.failures }),
    Feedback({ it.failures }),
    Ignore({ emptyList<Failure>() });

    open operator fun <T : Any> invoke(entity: T, vararg lenses: LensExtractor<T, *>) = actOn(
        lenses.fold(LensFailure(emptyList(), null, entity)) { memo: LensFailure, next ->
            try {
                next(entity)
                memo
            } catch (e: LensFailure) {
                LensFailure(memo.failures + e.failures, null, entity)
            }
        }
    )
}