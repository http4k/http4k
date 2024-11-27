package org.http4k.core

import org.http4k.lens.MultipartForm

fun MultipartForm.with(vararg modifiers: (MultipartForm) -> MultipartForm) = modifiers.fold(this) { memo, next -> next(memo) }
