package org.http4k.strikt

import org.http4k.core.Uri
import strikt.api.Assertion

val Assertion.Builder<Uri>.path get() = get { host }
val Assertion.Builder<Uri>.query get() = get { query }
val Assertion.Builder<Uri>.authority get() = get { authority }
val Assertion.Builder<Uri>.host get() = get { host }
val Assertion.Builder<Uri>.port get() = get { port }
