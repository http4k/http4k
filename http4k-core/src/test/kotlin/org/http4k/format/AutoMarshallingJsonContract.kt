package org.http4k.format

abstract class AutoMarshallingJsonContract(marshaller: AutoMarshalling) : AutoMarshallingContract(marshaller) {
    override val expectedAutoMarshallingResult = """{"string":"hello","child":{"string":"world","child":null,"numbers":[1],"bool":true},"numbers":[],"bool":false}"""
    override val expectedAutoMarshallingResultPrimitives = """{"duration":"PT1S","localDate":"2000-01-01","localTime":"01:01:01","localDateTime":"2000-01-01T01:01:01","zonedDateTime":"2000-01-01T01:01:01Z[UTC]","offsetTime":"01:01:01Z","offsetDateTime":"2000-01-01T01:01:01Z","instant":"1970-01-01T00:00:00Z","uuid":"1a448854-1687-4f90-9562-7d527d64383c","uri":"http://uri:8000","url":"http://url:9000","status":200}"""
}

