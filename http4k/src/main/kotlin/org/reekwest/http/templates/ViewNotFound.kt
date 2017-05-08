package org.http4k.http.templates

data class ViewNotFound(val view: ViewModel) : Exception("Template ${view.template()} not found")