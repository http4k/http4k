title: How to use the http4k docs
description: An explanation of how the http4k docs are organised

The http4k technical documentation has been designed following the Grand Unified Theory of Documentation. Overall, the http4k developers firmly believe that API design should be natural and friendly to the user, and hence the codebase is not heavily commented. If we have done our jobs correctly, someone with the correct knowledge of a particular domain or platform should be able to implement systems using the APIs by just using an IDE.

That said, there remains a lot to be written to ensure that the basic concepts of the toolkit are written down, and that users can leverage the significant number of features that http4k provides.

You can read more about the theory [here](https://documentation.divio.com/), but essentially there are four distinct styles of useful documentation, based on what mode the reader is operating in.

<img alt="grand unified theory of documentation" class="blogImage" src="/img/doc-system.png">

Regardless of which section you are reading, as much of the code as possible exists in the repository and is built with the rest of http4k in our CI. This has the effect of a making the code more verbose (including import statements and similar), but at the same time we can guarantee that the code compiles and you can navigate around it to find where everything is coming from. We hope you agree that this tradeoff is worth it.

### Concepts
http4k is a simple framework based around several function types, and hopefully the ideas behind it are not difficult to grasp. This section conveys the mindset and rationale behind http4k, and lays out each of the main function types used in the toolkit and how they relate to each other.

We recommend that all new users familiarise themselves with at least the [rationale](/concepts/rationale) and [HTTP](/concepts/http) pages of this section.

### Tutorials
Getting started with a new library can be quite daunting, and sometimes everyone needs a little hand holding to get comfortable with how things fit together. This section contains step-by-step guides to get you started with each of http4k's main conceptual areas. The [Quickstart](/tutorials/quickstart) will get you out of the gate and up and running in no time.

### How-to guides
The meat of the http4k documentation is in this section, in which you'll find ready made solutions to many common problems. Because if you've got something to achieve - it's pretty much guaranteed that we've probably already come across. 

The format for the recipes contains:
- Required Gradle dependencies
- A brief description of the problem
- Fully runnable code example displaying the solution.

Think of it like a mini StackOverflow. :) But better because you've got the entire solution available to adapt to your particular use-case.

As a community-driven project, we would welcome new or updated recipes to make http4k easier to use. The format of the new and updated recipes should follow [this Markdown template](https://github.com/http4k/http4k/blob/master/.github/RECIPE_TEMPLATE.md).

### Reference
In order to "fly like a butterfly and sting like a bee", http4k is heavily modularised. This section contains more detailed technical notes on the capabilities present each of the http4k modules. It's more of a "what" than a "why".

[http4k]: https://http4k.org
