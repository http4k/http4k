package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri


fun path(expected: String) = object : Matcher<Uri> {
    override val description = "Uri with Path $expected"
    override fun invoke(actual: Uri) = equalTo(expected)(actual.path)
}

fun query(expected: String) = object : Matcher<Uri> {
    override val description = "Uri with Query $expected"
    override fun invoke(actual: Uri) = equalTo(expected)(actual.query)
}

fun authority(expected: String) = object : Matcher<Uri> {
    override val description = "Uri with Authority $expected"
    override fun invoke(actual: Uri) = equalTo(expected)(actual.authority)
}

fun host(expected: String) = object : Matcher<Uri> {
    override val description = "Uri with Host $expected"
    override fun invoke(actual: Uri) = equalTo(expected)(actual.host)
}

fun port(expected: Int?) = object : Matcher<Uri> {
    override val description = "Uri with Port $expected"
    override fun invoke(actual: Uri) = equalTo(expected)(actual.port)
}