package org.http4k.security.oauth.server

import org.http4k.core.Response
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.State
import org.http4k.security.fragmentParameter

interface ResponseRender {

    fun withState(state: State?) = if (state == null || state.value.isBlank()) this else addParameter("state", state.value)

    fun withDocumentationUri(documentationUri: String?) = if (documentationUri.isNullOrEmpty()) this else addParameter("error_uri", documentationUri)

    fun addParameter(key: String, value: String?): ResponseRender

    fun complete(): Response

    companion object {
        fun forAuthRequest(authorizationRequest: AuthRequest) =
            forAuthRequest(authorizationRequest.responseMode, authorizationRequest.responseType, authorizationRequest.redirectUri!!)

        fun forAuthRequest(responseMode: ResponseMode?, responseType: ResponseType, redirectUri: Uri) =
            when (responseMode) {
                ResponseMode.Query -> QueryResponseRender(redirectUri)
                ResponseMode.Fragment -> FragmentResponseRender(redirectUri)
                null -> if (responseType == ResponseType.CodeIdToken) {
                    FragmentResponseRender(redirectUri)
                } else {
                    QueryResponseRender(redirectUri)
                }

            }
    }
}

class QueryResponseRender(private val uri: Uri) : ResponseRender {

    override fun addParameter(key: String, value: String?): ResponseRender = QueryResponseRender(uri.query(key, value))

    override fun complete(): Response = Response(SEE_OTHER).header("Location", uri.toString())

}

class FragmentResponseRender(private val uri: Uri) : ResponseRender {

    override fun addParameter(key: String, value: String?): ResponseRender = FragmentResponseRender(uri.fragmentParameter(key, value))

    override fun complete(): Response = Response(SEE_OTHER).header("Location", uri.toString())

}
