package org.reekwest.http.contract

object FormField : BiDiLensSpec<WebForm, String, String>("form field",
    MappableGetLens({ name, (fields) -> fields.getOrDefault(name, listOf()) }, { it }),
    MappableSetLens({ name, values, target -> values.fold(target, { m, next -> m.plus(name to next) }) }, { it })
)