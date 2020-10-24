title:Part 4: Adding an external dependency

description: A step-by-step guide to TDDing a simple http4k application

At this point, the separation of the layers starts to become clear:
- The server layer is responsible for taking external configuration and instantiating the app layer.
- The application layer API is only in terms of HTTP transports - it constructs business level abstractions
which are passed down into to the individual endpoints

The process here is to create fake versions of the dependency which can be tested against through the business interface.
This requires another style of testing, CDCs (Consumer Driven Contracts), to be created. These contract tests ensure that our
interactions with the external service are valid.

### Requirements:
- Results from calculations should be POSTed via HTTP to another "answer recording" service.

### Implementation Notes:
The following process is followed to us to the final state, whilst always allowing us to keep the build green:

1. Determine the HTTP contract required by the Recorder (in this case an HTTP POST to /{answer}
1. Create RecorderCdc and RealRecorderTest and make it pass for the real dependency by implementing the Recorder
1. Create FakeRecorderTest and FakeRecorderHttp and make it pass for the fake. We can now use the Fake to implement our requirement
1. Include the FakeRecorderHttp in the setup of EndToEndTest, starting and stopping the server (even though it's not doing anything)
1. Pass the configuration of the Recorder (baseUri) into the MyMathServer, which uses it to create the recorder HttpHandler
1. Factor AppEnvironment out of the functional tests. This is where all the setup of the functional testing environment will be done
1. Introduce the recorder HttpHandler to MyMathApp, creating a FakeRecorderHttp in the AppEnvironment
1. Alter the AddFunctionalTest and MultiplyFunctionalTest to set the expectations on the interactions recorder in FakeRecorderHttp
1. In MyMathApp, create the Recorder business implementation (Recorder) and pass it to calculate(), then implement the call to record()

### Tests:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/tutorials/tdding_http4k/_4/tests.kt"></script>

### Production:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/tutorials/tdding_http4k/_4/project.kt"></script>
