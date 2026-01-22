package org.http4k.template

class ViewNotFound(view: ViewModel, cause: Throwable? = null) :
    RuntimeException("Template ${view.template()} not found", cause)
