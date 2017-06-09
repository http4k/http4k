The creators of **http4k** takes testing very seriously - so seriously that there really isn't that much to say here! 
The API has been designed to make it as simple as possible to test both individual endpoints and entire applications in a consistent fashion, which is aided by remembering that:

1. The input and output `Request/Response` objects are immutable.
2. `HttpHandler` endpoints are just functions.
3. An entire **http4k** application is *just* an `HttpHandler`.

Because of the above, there really isn't much required in the way of "testing infrastructure" - no magic containers or test fixtures that you might find in other frameworks. 
Testing is just matter of calling the correct function! Additionally, because the server and client HttpHandler interfaces are symmetrical - moving between in and out of container contexts 
(or indeed even to another HTTP framework entirely) is just a matter of switching out the HttpHandler implementation from the constructed app (out of container) to an HTTP client (in-container).

That said, possibly the most useful thing is to demonstrate the process that we have developed to test micro-services. A simple example of the development process can be found 
[here](https://github.com/http4k/http4k/tree/master/src/test/kotlin/worked_example).
