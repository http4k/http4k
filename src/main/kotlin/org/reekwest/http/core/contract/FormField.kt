package org.reekwest.http.core.contract

object FormField : StringLensSpec<ValidatingForm>("form",
    { (fields), name -> fields[name] },
    { form, name, values -> values.fold(form, { m, next -> m.plus(name to next) }) })