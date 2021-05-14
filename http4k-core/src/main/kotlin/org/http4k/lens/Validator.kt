package org.http4k.lens

/**
 * Runs through a list of lenses and extracts the values from each one, collecting the errors
 */
enum class Validator(private val actOn: (LensFailure) -> List<Failure>) {
    Strict({ if (it.failures.isNotEmpty()) throw it else it.failures }),
    Feedback({ it.failures }),
    Ignore({ emptyList<Failure>() });

    operator fun <T : Any> invoke(entity: T, lenses: List<LensExtractor<T, *>>): List<Failure> =
        collectErrors(lenses, entity).run {
            actOn(when (size) {
                0 -> LensFailure()
                1 -> first()
                else -> LensFailure(flatMap { it.failures }, LensFailures(this), entity)
            })
        }

    private fun <T : Any> collectErrors(lenses: List<LensExtractor<T, *>>, entity: T): List<LensFailure> =
        lenses.fold(emptyList()) { memo, next ->
            try {
                next(entity)
                memo
            } catch (e: LensFailure) {
                memo + e
            }
        }
}

data class LensFailures(val causes: List<LensFailure>) : RuntimeException()
