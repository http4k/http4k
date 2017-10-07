package org.http4k.template

data class ViewNotFound(private val view: ViewModel) : Exception("Template ${view.template()} not found")