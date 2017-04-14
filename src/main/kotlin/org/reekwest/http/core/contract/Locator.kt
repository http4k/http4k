package org.reekwest.http.core.contract

interface Locator<IN, OUT> {
    val name: String
    fun get(target: IN, name: String): List<OUT?>?
    fun set(target: IN, name: String, values: List<OUT>): IN
}