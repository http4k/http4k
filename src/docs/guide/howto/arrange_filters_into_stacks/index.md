title: http4k How-to: Arrange Filters into stacks
description: A strategy for composing your applications

http4k Filters are just decorator functions for HttpHandlers and process requests by applying the following process:

1. Receive the Request
2. Modify it
3. Pass it to the next HttpHandler in the chain
4. Receive the Response
5. Modify it
6. Return it to the caller

We can reason that we can combine filters together to form chains, or "Stacks" of processing logic - moving from the most generic to the most specific. But the ordering
of the filters is important in order that we have the information at the point in the stack when we need it. For example - if we want to record all HTTP traffic, we much ensure that we 
do this after any exception handling has occurred (so that we can record the 5XX properly). Experience has shown that there is a general formula to be used when constructing stacks.

### Serverside

A typical stack looks like:

1. Debugging/Tracing <-- to ensure that we see all traffic
2. Reporting & metrics capture <-- to record accurately what we sent back
3. Catch unexpected exceptions <-- to ensure that all responses are handled by the application instead of the runtime
4. Catching expected exceptions <-- for instance LensFailures which are converted to 400s
5. Routing <-- if we want to route traffic based on the request (eg. `routes()`)
6. HttpHandlers <-- to process the traffic

### Clientside

The client-side is similar, but simpler:

1. Debugging/Tracing <-- to ensure that we see all traffic
2. Reporting & metrics capture <-- to record accurately what we sent back
3. Routing <-- if we want to route traffic based on the request (eg. `reverseProxy()`)
4. Http client <-- to process the traffic

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.25.0.0"))
    implementation("org.http4k:http4k-core")
}
```

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/arrange_filters_into_stacks/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/arrange_filters_into_stacks/example.kt"></script>
