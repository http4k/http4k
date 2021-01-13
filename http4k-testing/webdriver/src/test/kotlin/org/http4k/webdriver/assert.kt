package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws

fun isNotImplemented(fn: () -> Unit) = assertThat({ fn() }, throws<FeatureNotImplementedYet>())

