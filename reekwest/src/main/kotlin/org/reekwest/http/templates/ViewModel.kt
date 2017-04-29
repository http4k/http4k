package org.reekwest.http.templates

interface ViewModel {
    fun template(): String = javaClass.name.replace('.', '/')
}