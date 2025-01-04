package org.http4k.testing

import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.lens.contentType
import java.time.Duration

/**
 * Filter which injects a script into HTML responses using an event source to detect changes and reload the page.
 */
fun ReloadProxy(sleeper: (Duration) -> Unit = Thread::sleep): Filter {
    val reloadEventSourcePath = "/http4k/hot-reload"

    return InsertHotReloadScript(reloadEventSourcePath)
        .then(BlockConnection(reloadEventSourcePath, sleeper))
}

/**
 * Filter which blocks requests to a given path for a specified duration.
 */
fun BlockConnection(path: String, sleeper: (Duration) -> Unit) = Filter { next ->
    {
        if (it.uri.path == path) {
            sleeper(Duration.ofHours(1))
            Response(OK)
        } else next(it)
    }
}

/**
 * Filter which injects a script into HTML responses using an event source to detect changes and reload the page.
 */
fun InsertHotReloadScript(path: String) = Filter { next ->
    {
        next(it).run {
            when (TEXT_HTML) {
                contentType() -> {
                    val newBody = bodyString().replace("</body>", "</body>${reloadScript(path)}")
                    body(newBody)
                        .replaceHeader("Content-Length", newBody.length.toString())
                }

                else -> this
            }
        }
    }
}

/**
 * Script which connects to an event source and reloads the page when a message is received or connection closed.
 */
private fun reloadScript(path: String) =
    """
<script>
(function() {
    function connect() {
        const es = new EventSource('$path');
        
        es.onmessage = function(event) {
            window.location.reload();
        };

        es.onerror = function(error) {
            es.close();
            setTimeout(function() {
                window.location.reload();
                connect();
            }, 500);
        };
    }

    // Start initial connection attempt
    connect();
})();
</script>
"""
