title: http4k Contributors' Guide
description: http4k Contributor's Guide

<h2 class="github">Contributors' Guide</h2>

There are many ways in which you can contribute to the development of the library:

- Give us a Star on Github - you know you want to ;)
- Bugs! We don't want them, but we surely want to know about them so we can mercilessly squash them.
- Consider becoming a Supporter or a Backer through our OpenCollective (see below).

### pull requests
If there are any message format library or templating engine bindings that you'd like to see supported, then please feel free to suggest them or provide a PR. 

- JSON formats: create a new module with an implementation of `Json` by following the `Argo` example in the source.
- Templating engines: create a new module with a `Templates` implementation by following the `HandlebarsTemplates` example in the source

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

## Contributors

This project exists thanks to all the people who contribute.
<a href="https://github.com/http4k/http4k/graphs/contributors"><img src="https://opencollective.com/http4k/contributors.svg?width=890" /></a>

## Backers & Sponsors

If you use http4k in your project or enterprise and would like to support ongoing development, please consider becoming a backer or a sponsor. Sponsor logos will show up here with a link to your website.

<a href="https://opencollective.com/http4k/sponsor/1/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/1/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/2/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/2/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/3/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/3/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/4/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/4/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/5/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/5/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/6/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/6/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/7/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/7/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/8/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/8/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/9/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/9/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/0/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/0/avatar.svg"></a>
<a href="https://opencollective.com/http4k#backers" target="_blank"><img src="https://opencollective.com/http4k/backers.svg?width=500"></a>
