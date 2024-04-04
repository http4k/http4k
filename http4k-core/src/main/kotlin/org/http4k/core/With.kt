package org.http4k.core

import org.http4k.lens.WebForm

fun WebForm.with(vararg modifiers: (WebForm) -> WebForm) = modifiers.fold(this) { memo, next -> next(memo) }
