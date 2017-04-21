package org.reekwest.http.contract

object FormField : BiDiLensSpec<WebForm, String, String>("form field",
    GetLens({ name, (fields) -> fields.getOrDefault(name, listOf()) }, { it }),
    SetLens({ name, values, target -> values.fold(target, { m, next -> m.plus(name to next) }) }, { it })
)