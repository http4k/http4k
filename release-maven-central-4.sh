#!/bin/bash

source ./release-functions.sh

maven_publish "http4k-format-jackson-xml"
maven_publish "http4k-format-jackson-yaml"
maven_publish "http4k-format-xml"
maven_publish "http4k-format-kotlinx-serialization"

maven_publish "http4k-template-handlebars"
maven_publish "http4k-template-pebble"
maven_publish "http4k-template-thymeleaf"
maven_publish "http4k-template-freemarker"
maven_publish "http4k-template-jade4j"
maven_publish "http4k-template-dust"

maven_publish "http4k-bom"
