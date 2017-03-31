package org.reekwest.http.core

val APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded"

typealias Form = Parameters

fun Request.form(s: String): String? = extract(FormEntity).findSingle(s)

fun Form.toEntity() = Entity(toUrlEncoded())

object FormEntity : EntityExtractor<Form> {
    override fun invoke(request: HttpMessage): Form {
        if (request.header("content-type") != APPLICATION_FORM_URLENCODED) return listOf()
        return request.entity?.toString()?.toParameters() ?: emptyList()
    }
}