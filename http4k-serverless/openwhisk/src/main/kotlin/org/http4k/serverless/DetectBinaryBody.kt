package org.http4k.serverless

import org.http4k.core.Request
import org.http4k.core.Response

/**
 * OpenWhisk Base64 encodes Binary requests and responses when they are sent to
 * the deployed Function. This interface allows for custom implementations of that logic,
 * which might be required if your function supports more than one endpoint (with mixed
 * request/response types).
 */
interface DetectBinaryBody {
    fun isBinary(request: Request): Boolean
    fun isBinary(request: Response): Boolean

    companion object {
        val BinaryRequestOnly = object : DetectBinaryBody {
            override fun isBinary(request: Request) = true
            override fun isBinary(request: Response) = false
        }
        val BinaryResponseOnly = object : DetectBinaryBody {
            override fun isBinary(request: Request) = false
            override fun isBinary(request: Response) = true
        }
        val NonBinary = object : DetectBinaryBody {
            override fun isBinary(request: Request) = false
            override fun isBinary(request: Response) = false
        }
        val Binary = object : DetectBinaryBody {
            override fun isBinary(request: Request) = true
            override fun isBinary(request: Response) = true
        }
    }
}
