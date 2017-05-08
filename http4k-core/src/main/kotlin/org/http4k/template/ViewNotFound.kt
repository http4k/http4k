package org.http4k.template

data class ViewNotFound(val view: ViewModel) : Exception("Template ${view.template()} not found")