package org.reekwest.http.templates

data class ViewNotFound(val view: ViewModel) : Exception("Template ${view.template()} not found")