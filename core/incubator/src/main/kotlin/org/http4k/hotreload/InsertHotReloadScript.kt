package org.http4k.hotreload

import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Filter
import org.http4k.lens.contentType

/**
 * Filter which injects a script into HTML responses using an event source to detect changes and reload the page.
 */
fun InsertHotReloadScript(path: String): Filter {

    /**
     * Script which connects to an event source and reloads the page when a message is received or connection closed.
     */
    fun reloadScript(path: String) =
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
        
        es.onclose = function(error) {
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

    return Filter { next ->
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
}
