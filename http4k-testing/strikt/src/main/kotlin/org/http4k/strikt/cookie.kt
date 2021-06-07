package org.http4k.strikt

import org.http4k.core.cookie.Cookie
import strikt.api.Assertion

val Assertion.Builder<Cookie>.name get() = get(Cookie::name)
val Assertion.Builder<Cookie>.value get() = get(Cookie::value)
val Assertion.Builder<Cookie>.domain get() = get(Cookie::domain)
val Assertion.Builder<Cookie>.path get() = get(Cookie::path)
val Assertion.Builder<Cookie>.secure get() = get { secure }
val Assertion.Builder<Cookie>.httpOnly get() = get { httpOnly }
val Assertion.Builder<Cookie>.expires get() = get(Cookie::expires)
val Assertion.Builder<Cookie>.maxAge get() = get(Cookie::maxAge)
val Assertion.Builder<Cookie>.sameSite get() = get(Cookie::sameSite)
