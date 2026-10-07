<h2 class="github">Contributors' Guide</h2>

There are many ways in which you can contribute to the development of the library:

- Give us a Star on Github - you know you want to. ;)
- Using http4k to build something? Get in touch and tell everyone about it, or even just us!
- [Sponsor us!](https://github.com/sponsors/http4k) The http4k team build the library out of love for software engineering and the OpenSource community, but running a project of this size is not without it's costs. Please see below for sponsorship options to help us keep the project running.
- [Get help!](https://www.http4k.org/solutions/) The http4k team have produced a reasonable amount of training materials and are available for onsite or remote consulting engagements to help companies get the most out of the library.

### Pull requests
If there are any message format library or templating engine bindings that you'd like to see supported, then please feel free to suggest them or provide a PR. 

- JSON formats: create a new module with an implementation of `Json` by following the `Argo` example in the source.
- Templating engines: create a new module with a `Templates` implementation by following the `HandlebarsTemplates` example in the source.
- Server implementations: create a new module with a `Server` implementation by following the `Jetty` example in the source.
- Client implementations: create a new module with a `Client` implementation by following the `OkHttp` example in the source.

### Developer Certificate of Origin (DCO)

All commits must be signed off. Sign-off is a line at the end of the commit message:

```
Signed-off-by: Your Name <your@email.example>
```

By adding it you certify that you wrote the patch, or otherwise have the right to submit it
under the project's licence — the terms of the [Developer Certificate of Origin 1.1](https://developercertificate.org),
reproduced in full below.

**This is not a copyright assignment and it is not a CLA.** You keep the copyright in your
contribution. You are only stating that you are allowed to contribute it.

#### Signing off

Git writes the trailer for you with `-s`:

```shell
git commit -s -m "your message"
```

The sign-off email must match the commit author's email, so set these first:

```shell
git config user.name  "Your Name"
git config user.email "your@email.example"
```

Use `--global` to set them for every repository. If you use a GitHub noreply address, take
the one shown under [Settings → Emails](https://github.com/settings/emails).

Already committed without a sign-off? Rewrite the commits and force-push:

```shell
# a single commit
git commit --amend --signoff

# more than one
git rebase --signoff origin/master

git push --force-with-lease
```

A GitHub Actions check enforces this on every pull request. It skips merge commits and
known bots, and it fails if a commit is signed off by someone other than its author.

#### Developer Certificate of Origin 1.1

```
Developer Certificate of Origin
Version 1.1

Copyright (C) 2004, 2006 The Linux Foundation and its contributors.

Everyone is permitted to copy and distribute verbatim copies of this
license document, but changing it is not allowed.


Developer's Certificate of Origin 1.1

By making a contribution to this project, I certify that:

(a) The contribution was created in whole or in part by me and I
    have the right to submit it under the open source license
    indicated in the file; or

(b) The contribution is based upon previous work that, to the best
    of my knowledge, is covered under an appropriate open source
    license and I have the right under that license to submit that
    work with modifications, whether created in whole or in part
    by me, under the same open source license (unless I am
    permitted to submit under a different license), as indicated
    in the file; or

(c) The contribution was provided directly to me by some other
    person who certified (a), (b) or (c) and I have not modified
    it.

(d) I understand and agree that this project and the contribution
    are public and that a record of the contribution (including all
    personal information I submit with it, including my sign-off) is
    maintained indefinitely and may be redistributed consistent with
    this project or the open source license(s) involved.
```

### General guidelines
- Questions can be directed towards the [Slack #http4k](http://slack.kotlinlang.org/) channel, or on Twitter <a href="https://twitter.com/http4k">@http4k</a>
- For issues, please describe giving as much detail as you can - including version and steps to recreate
- At the moment, PRs should be sent to the master branch - this might change in future so check back everytime!
- Source/binary compatibility always must be kept as far as possible - this is a must for minor and patch versions
- PR changes should have test coverage. Note that we use Junit 5 as a test engine - which uses new `@Test` annotations.
- All the PRs must pass the GitHub CI jobs before merging them
- All commits must be signed off with `git commit -s` - see [Developer Certificate of Origin](#developer-certificate-of-origin-dco) above

https://github.com/http4k/http4k

Testing with default settings is required when push changes. Note that we currently build against Java 25 ([jEnv](https://www.jenv.be/) is good for managing multiple java versions):

```shell
./gradlew check
```

<h2 class="github">

## Appreciation
We love our community! See [the http4k site](https://http4k.org/community/) for details!

</h2>
