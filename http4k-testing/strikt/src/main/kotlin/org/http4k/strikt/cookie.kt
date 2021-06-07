package org.http4k.strikt

import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import strikt.api.Assertion
import java.time.LocalDateTime

val Assertion.Builder<Cookie>.name get() = get { name }
val Assertion.Builder<Cookie>.value get() = get { value }
val Assertion.Builder<Cookie>.domain get() = get { domain }
val Assertion.Builder<Cookie>.path get() = get { path }
val Assertion.Builder<Cookie>.secure get() = get { secure }
val Assertion.Builder<Cookie>.httpOnly get() = get { httpOnly }
val Assertion.Builder<Cookie>.expires get() = get { expires }
val Assertion.Builder<Cookie>.maxAge get() = get { maxAge }
val Assertion.Builder<Cookie>.sameSite get() = get { sameSite }
