package org.http4k.strikt

import org.http4k.lens.Lens
import org.http4k.lens.WebForm
import strikt.api.Assertion

fun <T> Assertion.Builder<WebForm>.field(lens: Lens<WebForm, T>) = get { lens(this) }
