The creators of **http4k** takes testing very seriously - so seriously that there really isn't that much to say here! 
The API has been designed to make it as simple as possible to test both individual endpoints and entire applications in a consistent fashion, which is aided by remembering that:

1. The input and output `Request/Response` objects are immutable.
1. `HttpHandlers` are just functions.
1. An entire **http4k** application is *just* an `HttpHandler`.

Because of the above, there really isn't much required in the way of "testing infrastructure" - no magic containers or test fixtures that you might find in other frameworks. 
Testing is just matter of calling the correct function! Additionally, because the server and client HttpHandler interfaces are symmetrical - moving between in and out of container contexts 
(or indeed even to another HTTP framework entirely) is just a matter of switching out the HttpHandler implementation from the constructed app (out of container) to an HTTP client (in-container).

That said, possibly the most useful thing is to demonstrate the process that we have developed to test micro-services. A simple example of the development process can be found 
[here](/guide/example).

## Testing modules
We have developed the following modules to help with testing:

- [http4k-testing-hamkrest](/guide/modules/hamkrest): a set of composable Hamkrest matchers for matching **http4k** message objects against.
- [http4k-testing-webdriver](/guide/modules/webdriver): an ultra-lightweight Selenium WebDriver implementation which can be used to test-drive **http4k** apps (ie. HttpHandlers).

## Example code for testing

#### Testing HttpHandlers with static paths
<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/testing/DynamicPathTest.kt"></script>

#### Testing HttpHandlers with dynamic paths
<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/testing/StaticPathTest.kt"></script>

#### Testing Filters
<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/testing/FilterTest.kt"></script>
