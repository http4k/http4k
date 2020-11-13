package org.http4k.template

data class ViewNotFound(private val view: ViewModel) : RuntimeException("Template ${view.template()} not found")
