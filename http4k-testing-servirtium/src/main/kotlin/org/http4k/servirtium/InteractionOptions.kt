package org.http4k.servirtium

import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response

/**
 * This controls how the Servirtium interactions are recorded to the storage format. The manipulations are
 * used to replace/remove any dynamic parts of the request (eg. Date headers) so that the traffic can be
 * correctly matched during the replay process.
 */
interface InteractionOptions {

    /**
     * Modify received requests before they are stored
     */
    fun modify(request: Request): Request = request

    /**
     * Modify received responses before they are stored
     */
    fun modify(response: Response): Response = response

    /**
     * Determine if the content type should be treated as binary. Binary messages are
     */
    fun isBinary(contentType: ContentType): Boolean = false

    companion object {
        object Defaults : InteractionOptions
    }
}
