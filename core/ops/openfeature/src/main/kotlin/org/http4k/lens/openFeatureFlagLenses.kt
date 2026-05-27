package org.http4k.lens

import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.core.Request
import org.http4k.filter.OPENFEATURE_CONTEXT_KEY
import org.http4k.lens.ParamMeta.ObjectParam

object OpenFeatureFlag {
    fun boolean() = OpenFeatureFlagLensSpec { it as? Boolean }
    fun string() = OpenFeatureFlagLensSpec { it as? String }
    fun int() = OpenFeatureFlagLensSpec { (it as? Number)?.toInt() }
    fun double() = OpenFeatureFlagLensSpec { (it as? Number)?.toDouble() }
    fun long() = OpenFeatureFlagLensSpec { (it as? Number)?.toLong() }
}

class OpenFeatureFlagLensSpec<OUT : Any> internal constructor(private val coerce: (Any?) -> OUT?) {

    fun defaulted(key: String, default: OUT): Lens<Request, OUT> = Lens(meta(key, required = true)) { req ->
        readValue(req, key)?.let(coerce) ?: default
    }

    fun optional(key: String): Lens<Request, OUT?> = Lens(meta(key, required = false)) { req ->
        readValue(req, key)?.let(coerce)
    }

    fun required(key: String): Lens<Request, OUT> {
        val m = meta(key, required = true)
        return Lens(m) { req ->
            val raw = readValue(req, key) ?: throw LensFailure(Missing(m), target = req)
            coerce(raw) ?: throw LensFailure(Invalid(m), target = req)
        }
    }

    fun <NEW : Any> map(forward: (OUT) -> NEW): OpenFeatureFlagLensSpec<NEW> =
        OpenFeatureFlagLensSpec { coerce(it)?.let(forward) }

    private fun readValue(req: Request, key: String): Any? {
        val flag = OPENFEATURE_CONTEXT_KEY(req).flags[FlagKey.of(key)] ?: return null
        return if (flag.errorCode != null) null else flag.value
    }
}

private fun meta(name: String, required: Boolean) =
    Meta(required, "openfeature", ObjectParam, name, null, emptyMap())
