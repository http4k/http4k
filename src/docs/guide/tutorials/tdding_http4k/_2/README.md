title: TDDing http4k Part 2: Adding an endpoint
description: A step-by-step guide to TDDing a simple http4k application

Starting with another EndToEnd test, we can then drill-down into the functional behaviour of the system by introducing
OCT (Out of Container) tests and converting the e2e test to just test endpoint wiring (so far). The common assertions have
also been converted to reusable extension methods on Response.

### Requirements:
- Implement an "add" service, which will sum a number of integer values.

### Tests:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/tdding_http4k/_2/tests.kt"></script>

### Production:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/tdding_http4k/_2/project.kt"></script>
