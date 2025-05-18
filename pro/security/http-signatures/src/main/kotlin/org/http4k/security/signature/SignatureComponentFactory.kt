package org.http4k.security.signature

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.signature.SignatureComponent.Authority
import org.http4k.security.signature.SignatureComponent.Header
import org.http4k.security.signature.SignatureComponent.Method
import org.http4k.security.signature.SignatureComponent.Path
import org.http4k.security.signature.SignatureComponent.Query
import org.http4k.security.signature.SignatureComponent.QueryParam
import org.http4k.security.signature.SignatureComponent.RequestTarget
import org.http4k.security.signature.SignatureComponent.Scheme
import org.http4k.security.signature.SignatureComponent.Status
import org.http4k.security.signature.SignatureComponent.TargetUri

/**
 * A factory for creating [SignatureComponent]s based on their name and parameters.
 *
 * @param Target The type of [HttpMessage] that the component is associated with.
 */
fun interface SignatureComponentFactory<Target : HttpMessage> {

    operator fun invoke(name: String, params: Map<String, String>): SignatureComponent<Target>?

    companion object {

        fun <Target : HttpMessage> NoCustomComponents() = SignatureComponentFactory<Target> { _, _ -> null }

        /**
         * Creates a [SignatureComponentFactory] for signing [Request] objects.
         *
         * @param customComponentFactory A factory for creating custom components.
         * @return A [SignatureComponentFactory] for [Request] objects.
         */
        fun HttpRequest(customComponentFactory: SignatureComponentFactory<Request> = NoCustomComponents()):
            SignatureComponentFactory<Request> = SignatureComponentFactory { name, params ->
            when (name) {
                Authority.name -> Authority
                Method.name -> Method
                Path.name -> Path
                Query.name -> Query
                QueryParam("-").name -> QueryParam(params["name"] ?: "")
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
         * Creates a [SignatureComponentFactory] for signing [Response] objects.
         *
         * @param customComponentFactory A factory for creating custom components.
         * @return A [SignatureComponentFactory] for [Response] objects.
         */
        fun HttpResponse(customComponentFactory: SignatureComponentFactory<Response> = NoCustomComponents()):
            SignatureComponentFactory<Response> = SignatureComponentFactory { name, params ->
            when (name) {
                Status.name -> Status
                Authority.name -> Authority
                Method.name -> Method
                Path.name -> Path
                Query.name -> Query
                QueryParam("-").name -> QueryParam(params["name"] ?: "")
                RequestTarget.name -> RequestTarget
                Scheme.name -> Scheme
                TargetUri.name -> TargetUri
                else -> when {
                    name.startsWith("@") -> customComponentFactory(name, params)
                    else -> Header(name, params)
                }
            }
        }
    }
}
