package org.http4k.traffic

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import java.io.File

/**
 * Tries to retrieve a stored response for a given request.
 */
fun interface Source {
    operator fun get(request: Request): Response?

    companion object {
        /**
         * Looks up traffic from the FS, based on tree storage format.
         */
        fun DiskTree(baseDir: String = ".") = Source {
            File(it.toFolder(baseDir.toBaseFolder()), "response.txt").run {
                if (exists()) Response.parse(String(readBytes())) else null
            }
        }

        /**
         * Looks up traffic from Memory, based on map storage format.
         */
        fun MemoryMap(cache: MutableMap<Request, Response>) = Source { cache[it] }
    }
}
