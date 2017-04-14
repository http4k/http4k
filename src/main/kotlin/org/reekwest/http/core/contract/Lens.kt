package org.reekwest.http.core.contract

abstract class Lens<in IN, OUT, FINAL>(val meta: Meta, private val spec: LensSpec<IN, OUT>) {
    operator fun invoke(target: IN): FINAL = try {
        convertIn(spec.get(target, meta.name))
    } catch (e: Missing) {
        throw e
    } catch (e: Exception) {
        throw Invalid(meta)
    }

    abstract internal fun convertIn(o: List<OUT?>?): FINAL
    abstract internal fun convertOut(o: FINAL): OUT

    operator fun <R : IN> invoke(target: R, value: FINAL): R = spec.set(target, convertOut(value))
}
