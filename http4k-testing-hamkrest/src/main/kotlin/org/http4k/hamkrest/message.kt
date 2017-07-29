package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.lens.Header

fun header(name: String, expected: String?) = object : Matcher<HttpMessage> {
    override val description = "HttpMessage with Header $name=$expected"
    override fun invoke(actual: HttpMessage) = equalTo(expected)(actual.header(name))
}

fun header(name: String, expected: List<String?>) = object : Matcher<HttpMessage> {
    override val description = "HttpMessage with Header $name=$expected"
    override fun invoke(actual: HttpMessage) = equalTo(expected)(actual.headerValues(name))
}

fun contentType(expected: ContentType) = object : Matcher<HttpMessage> {
    override val description = "HttpMessage with Content-Type $expected"
    override fun invoke(actual: HttpMessage) = equalTo(expected)(Header.Common.CONTENT_TYPE(actual))
}

fun body(expected: String) = object : Matcher<HttpMessage> {
    override val description = "HttpMessage with Body $expected"
    override fun invoke(actual: HttpMessage) = equalTo(expected)(actual.bodyString())
}