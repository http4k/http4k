package org.http4k.lens

enum class Validator {
    Strict {
        override fun actOn(errors: List<Failure>) {
            if (errors.isNotEmpty()) throw LensFailure(errors)
        }
    },
    Feedback {
        override fun actOn(errors: List<Failure>) {}
    };

    operator fun <T> invoke(entity: T, vararg formFields: Lens<T, *>): List<Failure> =
        formFields.fold(emptyList<Failure>()) { memo, next ->
            try {
                next(entity)
                memo
            } catch (e: LensFailure) {
                memo.plus(e.failures)
            }
        }.apply(this::actOn)

    abstract internal fun actOn(errors: List<Failure>)
}