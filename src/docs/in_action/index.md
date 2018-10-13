title: http4k in Action
description: Example applications and usages of http4k, including TDD approach 

# Rationale & design

### KotlinConf presentations: Server as a Function. In Kotlin. __________________
- [Slides](https://speakerdeck.com/daviddenton/server-as-a-function-in-kotlin)
- [Video](http://bit.ly/serverasafunction)

# See http4k in action in these example projects:

| Description | Templates | Testing | JSON | HttpClient | WS | AWS | CD pipeline | Contracts | Lambda/Graal |
|-----|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
|["Hello World"](https://start.http4k.org)| | | | | | |✔| | | | | |
|[TDD'd example application](https://github.com/http4k/http4k-contract-example-app)|✔|✔|✔| | | | |✔| |
|[Dropbox clone in 70 lines of Kotlin](https://github.com/daviddenton/http4kbox)|✔|✔|✔| | |✔|✔| |✔|
|[Simple websocket driven chat-server in 30 lines of Kotlin](https://github.com/daviddenton/http4k-demo-irc)| |✔| | |✔| |✔| | |
|[Stage-by-stage example of development process (London TDD style)](/guide/example)| |✔| | | | | | | |
|[Todo backend (standard routing version)](https://github.com/http4k/http4k-todo-backend)| | |✔| | | | | | |
|[Todo backend (contract routing version)](https://github.com/http4k/http4k-contract-todo-backend)| | |✔| | | | |✔| |
|[Real World example (Medium clone)](https://github.com/alisabzevari/kotlin-http4k-realworld-example-app)| | |✔| | | | | | |
