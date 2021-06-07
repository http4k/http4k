package org.http4k.strikt

import org.http4k.core.HttpTransaction
import strikt.api.Assertion

val Assertion.Builder<HttpTransaction>.duration get() = get { duration }
val Assertion.Builder<HttpTransaction>.request get() = get { request }
val Assertion.Builder<HttpTransaction>.response get() = get { response }
