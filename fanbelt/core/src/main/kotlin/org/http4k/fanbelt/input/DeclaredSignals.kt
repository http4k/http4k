package org.http4k.fanbelt.input

internal const val PAINTED = "fanbeltPainted"

internal class DeclaredSignals(signals: Any?) {
    private val declared = if (signals == null) emptyList() else SignalsJson.fields(SignalsJson.parse(SignalsJson.asFormatString(signals))).toList()
    private val errors = "errors" to SignalsJson.obj(declared.map { (name, _) -> name to SignalsJson.string("") })

    fun json(painted: Map<String, String> = emptyMap()): String {
        val paintedField = if (painted.isEmpty()) emptyList() else listOf(PAINTED to SignalsJson.obj(painted.mapValues { SignalsJson.string(it.value) }.toList()))
        return SignalsJson.compact(SignalsJson.obj(declared + errors + paintedField))
    }
}
