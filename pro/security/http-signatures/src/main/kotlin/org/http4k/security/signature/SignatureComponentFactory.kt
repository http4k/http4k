package org.http4k.security.signature

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.signature.SignatureComponent.*

/**
 * A factory for creating [SignatureComponent]s based on their name and parameters.
 *
 * @param M The type of [HttpMessage] that the component is associated with.
 */
fun interface SignatureComponentFactory<M : HttpMessage> {

    operator fun invoke(name: String, params: Map<String, String>): SignatureComponent<M>?

    companion object {

        /**
         * Creates a [SignatureComponentFactory] for [Request] objects.
         *
         * @param customComponentFactory A factory for creating custom components.
         * @return A [SignatureComponentFactory] for [Request] objects.
         */
        fun HttpRequest(customComponentFactory: SignatureComponentFactory<Request> = SignatureComponentFactory { _, _ -> null }):
                SignatureComponentFactory<Request> = SignatureComponentFactory { name, params ->
            when (name) {
                Authority.name -> Authority
                Method.name -> Method
                Path.name -> Path
                Query.name -> Query
                QueryParam("foo").name -> QueryParam(params["name"] ?: "")
                RequestTarget.name -> RequestTarget
                Scheme.name -> Scheme
                TargetUri.name -> TargetUri
                else -> when {
                    name.startsWith("@") -> customComponentFactory(name, params)
                    else -> Header(name, params)
                }
            }
        }

        /**
         * Creates a [SignatureComponentFactory] for [Response] objects.
         *
         * @param customComponentFactory A factory for creating custom components.
         * @return A [SignatureComponentFactory] for [Response] objects.
         */
        fun HttpResponse(customComponentFactory: SignatureComponentFactory<Response> = SignatureComponentFactory { _, _ -> null }):
                SignatureComponentFactory<Response> = SignatureComponentFactory { name, params ->
            when (name) {
                Status.name -> Status
                else -> when {
                    name.startsWith("@") -> customComponentFactory(name, params)
                    else -> Header(name, params)
                }
            }
        }
    }
}
