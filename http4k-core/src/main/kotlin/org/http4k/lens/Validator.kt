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

    operator fun <T> invoke(entity: T, vararg formFields: Lens<T, *>) =
        actOn(formFields.fold(listOf()) { memo, next ->
            try {
                next(entity)
                memo
            } catch (e: LensFailure) {
                memo.plus(e.failures)
            }
        })

    abstract internal fun actOn(errors: List<Failure>)
}