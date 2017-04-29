package org.reekwest.http.templates

interface View {
    fun template() = javaClass.name.replace('.', '/')
}