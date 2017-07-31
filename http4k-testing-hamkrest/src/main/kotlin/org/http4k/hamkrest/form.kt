package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.has
import org.http4k.lens.Lens
import org.http4k.lens.WebForm

fun <T> hasFormField(lens: Lens<WebForm, T>, matcher: Matcher<T>): Matcher<WebForm> = LensMatcher(has("Form Field '${lens.meta.name}'", { form: WebForm -> lens(form) }, matcher))
