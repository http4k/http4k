package org.reekwest.http.core.contract

object FormField : StringLensSpec<WebForm>("form",
    { (fields), name -> fields.getOrDefault(name, listOf()) },
    { form, name, values -> values.fold(form, { m, next -> m.plus(name to next) }) })