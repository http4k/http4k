package org.http4k.lens

enum class Validator(private val actOn: (List<Failure>) -> List<Failure>) {
    Strict({ if (it.isNotEmpty()) throw LensFailure(it) else it }),
    Feedback({ it }),
    Ignore({ emptyList<Failure>() });

    operator fun <T> invoke(entity: T, vararg formFields: Lens<T, *>): List<Failure> =
        formFields.fold(emptyList<Failure>()) { memo, next ->
            try {
                next(entity)
                memo
            } catch (e: LensFailure) {
                memo.plus(e.failures)
            }
        }.let(actOn)
}