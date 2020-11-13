#!/bin/bash

source ./release-functions.sh

maven_publish "http4k-testing-approval"
maven_publish "http4k-testing-chaos"
maven_publish "http4k-testing-hamkrest"
maven_publish "http4k-testing-kotest"
maven_publish "http4k-testing-servirtium"
maven_publish "http4k-testing-webdriver"

maven_publish "http4k-template-core"
maven_publish "http4k-template-handlebars"
maven_publish "http4k-template-pebble"
maven_publish "http4k-template-thymeleaf"
maven_publish "http4k-template-freemarker"
maven_publish "http4k-template-jade4j"
maven_publish "http4k-template-dust"

maven_publish "http4k-bom"
