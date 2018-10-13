title: http4k in Action
description: Example applications and usages of http4k, including TDD approach 

# Rationale & design

### KotlinConf presentations: Server as a Function. In Kotlin. __________________
- [Slides](https://speakerdeck.com/daviddenton/server-as-a-function-in-kotlin)
- [Video](http://bit.ly/serverasafunction)

# See http4k in action in these example projects:

| Description | Server | Templates | Testing | JSON | Database | HttpClient | WS | AWS | CD pipeline | Serverless | Graal |
|-----|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
|["Hello World"](https://start.http4k.org)|✔| | | | | | | |✔| | | |
|[Todo backend (standard routing version)](https://github.com/http4k/http4k-todo-backend)|✔| | |✔| | | | | | | |
|[Todo backend (contract routing version)](https://github.com/http4k/http4k-contract-todo-backend)|✔| | |✔| | | | | | | |
|[Simple websocket driven chat-server in 30 lines of Kotlin](https://github.com/daviddenton/http4k-demo-irc)|✔| | | | | |✔| |✔| | |
|[Dropbox clone in 70 lines of Kotlin](https://github.com/daviddenton/http4kbox)|✔|o|✔|o| | | |✔|o|✔|o|
|[TDD'd example application](https://github.com/http4k/http4k-contract-example-app)|✔|o|✔|o| |✔| | | | | |
|[Stage-by-stage example of development process (London TDD style)](/guide/example)|✔| | | | | | | | | | |
