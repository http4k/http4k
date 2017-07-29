package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri

fun hasUriPath(expected: String) = object : Matcher<Uri> {
    override val description = "Uri with Path $expected"
    override fun invoke(actual: Uri) = equalTo(expected)(actual.path)
}

fun hasUriQuery(expected: String) = object : Matcher<Uri> {
    override val description = "Uri with Query $expected"
    override fun invoke(actual: Uri) = equalTo(expected)(actual.query)
}

fun hasAuthority(expected: String) = object : Matcher<Uri> {
    override val description = "Uri with Authority $expected"
    override fun invoke(actual: Uri) = equalTo(expected)(actual.authority)
}

fun hasHost(expected: String) = object : Matcher<Uri> {
    override val description = "Uri with Host $expected"
    override fun invoke(actual: Uri) = equalTo(expected)(actual.host)
}

fun hasPort(expected: Int?) = object : Matcher<Uri> {
    override val description = "Uri with Port $expected"
    override fun invoke(actual: Uri) = equalTo(expected)(actual.port)
}