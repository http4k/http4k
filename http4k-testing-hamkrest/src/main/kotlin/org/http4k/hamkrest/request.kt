package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie

fun query(name: String, expected: String?) = object : Matcher<Request> {
    override val description = "Request with Query $name=$expected"
    override fun invoke(actual: Request) = equalTo(expected)(actual.query(name))
}

fun query(name: String, expected: List<String?>) = object : Matcher<Request> {
    override val description = "Request with Query $name=$expected"
    override fun invoke(actual: Request) = equalTo(expected)(actual.queries(name))
}

fun method(expected: Method) = object : Matcher<Request> {
    override val description = "Request with Method $expected"
    override fun invoke(actual: Request) = equalTo(expected)(actual.method)
}

fun uri(expected: Uri) = object : Matcher<Request> {
    override val description = "Request with Uri $expected"
    override fun invoke(actual: Request) = equalTo(expected)(actual.uri)
}

fun uri(expected: String) = object : Matcher<Request> {
    override val description = "Request with Uri $expected"
    override fun invoke(actual: Request) = equalTo(expected)(actual.uri.toString())
}

fun uri(expected: Matcher<Uri>) = object : Matcher<Request> {
    override val description = "Request with Uri $expected"
    override fun invoke(actual: Request) = expected(actual.uri)
}

fun cookie(expected: Cookie) = object : Matcher<Request> {
    override val description = "Request with Cookie $expected"
    override fun invoke(actual: Request) = equalTo(expected)(actual.cookie(expected.name))
}

fun cookie(name: String, expected: String) = object : Matcher<Request> {
    override val description = "Request with Cookie $name=$expected"
    override fun invoke(actual: Request) = equalTo(expected)(actual.cookie(name)?.value)
}

fun cookie(name: String, expected: Matcher<Cookie?>) = object : Matcher<Request> {
    override val description = "Request with Cookie $name"
    override fun invoke(actual: Request) = expected(actual.cookie(name))
}