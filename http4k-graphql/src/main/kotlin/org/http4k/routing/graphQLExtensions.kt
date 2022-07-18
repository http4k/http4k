package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.graphql.GraphQLHandler
import org.http4k.graphql.GraphQLRequest.Companion.requestLens
import org.http4k.graphql.GraphQLResponse.Companion.responseLens
import org.http4k.graphql.GraphQLWithContextHandler
import org.http4k.lens.LensFailure

/**
 * Routing plugin for GraphQL handling.
 */
fun graphQL(
    handler: GraphQLHandler,
    badRequestFn: (LensFailure) -> Response = { Response(BAD_REQUEST) },
): RoutingHttpHandler = routes(POST to {
    try {
        Response(OK).with(responseLens of handler(requestLens(it)))
    } catch (e: LensFailure) {
        badRequestFn(e)
    }
})

/**
 * Routing plugin for GraphQL handling with contextual data.
 */
fun <T> graphQL(
    handler: GraphQLWithContextHandler<T>,
    getContext: (Request) -> T,
    badRequestFn: (LensFailure) -> Response = { Response(BAD_REQUEST) },
): RoutingHttpHandler = routes(POST to {
    try {
        Response(OK).with(responseLens of handler(requestLens(it), getContext(it)))
    } catch (e: LensFailure) {
        badRequestFn(e)
    }
})

fun playground(playgroundPath: String = "/playground", graphQLRoute: String = "/graphql"): RoutingHttpHandler =
    playgroundPath bind GET to {
        val html = """
            <!DOCTYPE html>
            <html>

            <head>
              <meta charset=utf-8/>
              <meta name="viewport" content="user-scalable=no, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, minimal-ui">
              <title>GraphQL Playground</title>
              <link rel="stylesheet" href="//cdn.jsdelivr.net/npm/graphql-playground-react/build/static/css/index.css" />
              <link rel="shortcut icon" href="//cdn.jsdelivr.net/npm/graphql-playground-react/build/favicon.png" />
              <script src="//cdn.jsdelivr.net/npm/graphql-playground-react/build/static/js/middleware.js"></script>
            </head>

            <body>
              <div id="root">
                <style>
                  body {
                    background-color: rgb(23, 42, 58);
                    font-family: Open Sans, sans-serif;
                    height: 90vh;
                  }

                  #root {
                    height: 100%;
                    width: 100%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                  }

                  .loading {
                    font-size: 32px;
                    font-weight: 200;
                    color: rgba(255, 255, 255, .6);
                    margin-left: 20px;
                  }

                  img {
                    width: 78px;
                    height: 78px;
                  }

                  .title {
                    font-weight: 400;
                  }
                </style>
                <img src='//cdn.jsdelivr.net/npm/graphql-playground-react/build/logo.png' alt=''>
                <div class="loading"> Loading
                  <span class="title">GraphQL Playground</span>
                </div>
              </div>
              <script>window.addEventListener('load', function (event) {
                  GraphQLPlayground.init(document.getElementById('root'), {
                    // options as 'endpoint' belong here
                    endpoint: '$graphQLRoute'
                  })
                })</script>
            </body>

            </html>
        """.trimIndent()
        Response(OK).body(html)
    }
