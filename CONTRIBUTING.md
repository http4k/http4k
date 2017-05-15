# Contributers' Guide

There are many ways in which you can contribute to the development of the library:

- Give us a Star on Github - you know you want to ;)
- Questions can be directed towards the Gitter channel, or on Twitter <a href="https://twitter.com/http4k">@http4k</a>
- For issues, please describe giving as much detail as you can - including version and steps to recreate

### pull requests
If there are any message format library or templating engine bindings that you'd like to see supported, then please feel free to suggest them or provide a PR. 

- JSON formats: create a new module with an implementation of `Json` by following the `Argo` example in the source.
- Templating engines: create a new module with a `Templates`implementation by following the `HandlebarsTemplates` example in the source

### general guidelines
- At the moment, PRs should be sent to the master branch - this might change in future so check back everytime!
- Source/binary compatibility always must be kept as far as possible - this is a must for minor and patch versions
- PR changes should have test coverage
- All the PRs must pass the Travis CI jobs before merging them

https://travis-ci.org/http4k/http4k

Testing with default settings is required when push changes:

`sh
./gradlew check
`
