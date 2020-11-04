title: TDDing http4k Part 1: Building a walking skeleton
description: A step-by-step guide to TDDing a simple http4k application

Until we have an application that can be deployed, we cannot create any business value. The Walking Skeleton
model dictates that putting the most trivial endpoint into a production environment will prove our deployment
pipeline is sound, and helps to set the direction for the testing strategy that we will use going forward.

We start with in ICT (In-Container-Test), which have the job of testing server-level concerns such as monitoring,
documentation, and checking in a high-level way that the business endpoints are wired correctly.

### Requirements:
- The service can be pinged over HTTP to prove that is still alive.

### Tests:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/tdding_http4k/_1/tests.kt"></script>

### Production:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/tdding_http4k/_1/project.kt"></script>
